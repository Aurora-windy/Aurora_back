package com.aurora.ai.knowledge.graph;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.ai.knowledge.graph.Neo4jGraphService.GraphData;
import com.aurora.ai.knowledge.graph.Neo4jGraphService.GraphInfo;
import com.aurora.ai.knowledge.support.KnowledgePublishExecutorConfig;
import com.aurora.common.constant.PermCodeConst;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.aurora.common.response.Result;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * 知识图谱 REST 接口。
 * <p>图谱数据源自文档发布/上传后经 LLM 抽取的 (实体,关系,实体) 三元组，存储在 Neo4j。
 * 未启用或不可达时，查询接口返回空数据（status=closed），上传接口明确报错。</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/graph")
public class KnowledgeGraphController {

    private static final long MAX_FILE_BYTES = 2 * 1024 * 1024L; // 2MB
    private static final String GRAPH_DISABLED_MSG =
            "知识图谱未启用，请配置环境变量 AURORA_NEO4J_ENABLED=true 及 Neo4j 连接信息后重启后端";

    /** 任务状态最多保留条数，超出按 LRU 淘汰，避免长期运行内存无限增长 */
    private static final int MAX_TASK_HISTORY = 200;

    /** 异步抽取任务状态。status: PROCESSING / SUCCESS / FAILED / UNKNOWN */
    public record GraphTaskResp(String status, String message, int triples) {
    }

    private final Neo4jGraphService neo4jGraphService;

    @Qualifier(KnowledgePublishExecutorConfig.KNOWLEDGE_PUBLISH_EXECUTOR)
    private final Executor taskExecutor;

    private final Map<Long, GraphTaskResp> taskStore = Collections.synchronizedMap(
            new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, GraphTaskResp> eldest) {
                    return size() > MAX_TASK_HISTORY;
                }
            });

    /** 图谱连接状态与规模概览 */
    @SaCheckPermission(PermCodeConst.Ai.Graph.LIST)
    @GetMapping
    public Result<GraphInfo> info() {
        return Result.ok(neo4jGraphService.graphInfo());
    }

    /** 随机采样节点（用于初始渲染） */
    @SaCheckPermission(PermCodeConst.Ai.Graph.LIST)
    @GetMapping("/nodes")
    public Result<GraphData> nodes(@RequestParam(defaultValue = "100") int num) {
        return Result.ok(neo4jGraphService.sampleNodes(num));
    }

    /** 按实体名检索其邻居子图 */
    @SaCheckPermission(PermCodeConst.Ai.Graph.LIST)
    @GetMapping("/node")
    public Result<GraphData> searchNode(@RequestParam String entityName) {
        if (!StringUtils.hasText(entityName)) {
            throw new BizException(BizCode.PARAM_ERROR, "实体名称不能为空");
        }
        return Result.ok(neo4jGraphService.searchEntity(entityName));
    }

    /** 删除指定实体及其关联边 */
    @SaCheckPermission(PermCodeConst.Ai.Graph.LIST)
    @DeleteMapping("/node")
    public Result<Boolean> deleteNode(@RequestParam String entityName) {
        if (!StringUtils.hasText(entityName)) {
            throw new BizException(BizCode.PARAM_ERROR, "实体名称不能为空");
        }
        return Result.ok(neo4jGraphService.deleteEntity(entityName));
    }

    /** 清空整个图谱 */
    @SaCheckPermission(PermCodeConst.Ai.Graph.LIST)
    @DeleteMapping("/all")
    public Result<Boolean> deleteAll() {
        return Result.ok(neo4jGraphService.deleteAll());
    }

    /**
     * 上传文档入库图谱：读取文本 → 后台 LLM 抽取三元组 → 写入 Neo4j（不依赖向量库/知识库发布链路）。
     *
     * <p>抽取要跑一次 LLM 生成，耗时取决于供应商（慢时可达分钟级）。原实现同步等待，
     * 必然撞上前端 30 秒超时——这是「图谱上传总是超时」的直接原因。</p>
     *
     * <p>改为提交后台任务后立即返回 taskId，前端轮询
     * {@code GET /ai/graph/documents/{taskId}/status} 取结果。</p>
     */
    @SaCheckPermission(PermCodeConst.Ai.Graph.LIST)
    @PostMapping("/documents/upload")
    public Result<Map<String, Object>> uploadDocument(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(BizCode.PARAM_ERROR, "上传文件不能为空");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BizException(BizCode.PARAM_ERROR, "文件过大，请上传 2MB 以内的文本文件");
        }
        if (!neo4jGraphService.isEnabled()) {
            throw new BizException(BizCode.SYSTEM_ERROR, GRAPH_DISABLED_MSG);
        }
        String fileName = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename() : "unnamed.txt";
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (!(lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".markdown"))) {
            throw new BizException(BizCode.PARAM_ERROR, "仅支持 .txt / .md / .markdown 文本文件");
        }
        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BizException(BizCode.DOC_PROCESSING_FAILED, "读取文件失败：" + e.getMessage());
        }
        if (!StringUtils.hasText(content.trim())) {
            throw new BizException(BizCode.PARAM_ERROR, "文件内容为空");
        }
        final long docId = IdWorker.getId();
        final long taskId = IdWorker.getId();
        taskStore.put(taskId, new GraphTaskResp("PROCESSING", "", 0));
        // lambda 要求捕获 effectively final 的变量，显式转存以免后续改动破坏捕获
        final String taskFileName = fileName;
        final String taskContent = content;
        taskExecutor.execute(() -> {
            try {
                Neo4jGraphService.ExtractResult result = neo4jGraphService.extractAndStore(docId, taskFileName, taskContent);
                // reason 即"为什么没抽到"——把诊断信息回传给前端，用户不用翻日志也能定位
                String message = result.reason() != null ? result.reason() : "";
                taskStore.put(taskId, new GraphTaskResp("SUCCESS", message, result.triples()));
            } catch (Exception e) {
                log.error("图谱抽取任务失败 taskId={} docId={}", taskId, docId, e);
                taskStore.put(taskId, new GraphTaskResp("FAILED", String.valueOf(e.getMessage()), 0));
            }
        });
        return Result.ok(Map.of(
                "taskId", String.valueOf(taskId),
                "docId", String.valueOf(docId),
                "fileName", fileName,
                "chars", content.length()));
    }

    /** 查询异步抽取任务状态，供上传后轮询 */
    @SaCheckPermission(PermCodeConst.Ai.Graph.LIST)
    @GetMapping("/documents/{taskId}/status")
    public Result<GraphTaskResp> taskStatus(@PathVariable Long taskId) {
        GraphTaskResp resp = taskStore.get(taskId);
        return Result.ok(resp == null ? new GraphTaskResp("UNKNOWN", "任务不存在或已过期", 0) : resp);
    }
}
