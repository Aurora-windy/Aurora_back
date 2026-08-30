package com.aurora.ai.knowledge.service.impl;

import com.aurora.ai.knowledge.entity.AiKnowledgeChunkDO;
import com.aurora.ai.knowledge.entity.AiKnowledgeDocDO;
import com.aurora.ai.knowledge.mapper.AiKnowledgeChunkMapper;
import com.aurora.ai.knowledge.mapper.AiKnowledgeDocMapper;
import com.aurora.ai.knowledge.model.req.KnowledgeDocPageReq;
import com.aurora.ai.knowledge.model.req.KnowledgeDocSaveReq;
import com.aurora.ai.knowledge.model.resp.KnowledgeDocResp;
import com.aurora.ai.knowledge.model.resp.KnowledgePublishResp;
import com.aurora.ai.knowledge.graph.Neo4jGraphService;
import com.aurora.ai.knowledge.service.KnowledgeDocService;
import com.aurora.ai.knowledge.support.EmbeddingClient;
import com.aurora.ai.knowledge.support.KnowledgeChunker;
import com.aurora.ai.knowledge.support.KnowledgeDocStatus;
import com.aurora.ai.knowledge.support.KnowledgePublishExecutorConfig;
import com.aurora.ai.provider.entity.AiEmbeddingConfigDO;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.aurora.common.response.PageResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeDocServiceImpl implements KnowledgeDocService {

    /**
     * 单批向量化的分块数。/embeddings 的 input 支持数组，批量提交可把 N 次 HTTP 往返
     * 压到 N/16 次——这是上传超时优化中收益最大的一项（供应商不支持批量时客户端内部降级逐条）。
     */
    private static final int EMBED_BATCH_SIZE = 16;

    private final AiKnowledgeDocMapper docMapper;
    private final AiKnowledgeChunkMapper chunkMapper;
    private final KnowledgeChunker knowledgeChunker;
    private final EmbeddingClient embeddingClient;
    private final Neo4jGraphService neo4jGraphService;
    private final ObjectMapper objectMapper;

    @Qualifier(KnowledgePublishExecutorConfig.KNOWLEDGE_PUBLISH_EXECUTOR)
    private final Executor publishExecutor;

    @Override
    public PageResult<KnowledgeDocResp> page(KnowledgeDocPageReq req) {
        LambdaQueryWrapper<AiKnowledgeDocDO> wrapper = Wrappers.<AiKnowledgeDocDO>lambdaQuery()
                .like(StringUtils.hasText(req.getTitle()), AiKnowledgeDocDO::getTitle, req.getTitle())
                .eq(StringUtils.hasText(req.getType()), AiKnowledgeDocDO::getType, req.getType())
                .eq(StringUtils.hasText(req.getStatus()), AiKnowledgeDocDO::getStatus, req.getStatus())
                .orderByDesc(AiKnowledgeDocDO::getCreateTime);
        Page<AiKnowledgeDocDO> page = docMapper.selectPage(new Page<>(req.normalizedPageNum(), req.normalizedPageSize()), wrapper);
        return new PageResult<>(page.getRecords().stream().map(this::toResp).toList(), page.getTotal());
    }

    @Override
    public KnowledgeDocResp detail(Long id) {
        return toResp(requireDoc(id));
    }

    @Override
    public Long create(KnowledgeDocSaveReq req) {
        AiKnowledgeDocDO doc = new AiKnowledgeDocDO();
        fill(doc, req);
        doc.setStatus(KnowledgeDocStatus.DRAFT);
        doc.setVersion(1);
        docMapper.insert(doc);
        return doc.getId();
    }

    @Override
    public void update(Long id, KnowledgeDocSaveReq req) {
        AiKnowledgeDocDO doc = requireDoc(id);
        fill(doc, req);
        if (KnowledgeDocStatus.PUBLISHED.equals(doc.getStatus())) {
            doc.setStatus(KnowledgeDocStatus.DRAFT);
            doc.setPublishedAt(null);
        }
        doc.setVersion(doc.getVersion() == null ? 1 : doc.getVersion() + 1);
        docMapper.updateById(doc);
    }

    @Override
    public void updateFileUrl(Long id, String fileUrl) {
        AiKnowledgeDocDO doc = new AiKnowledgeDocDO();
        doc.setId(id);
        doc.setFileUrl(fileUrl);
        docMapper.updateById(doc);
    }

    @Override
    // 注意：本方法跨 MySQL(doc) 与 PG(chunk) 两库，不可加 @Transactional——
    // 事务会把连接钉死在首个数据源(master)，@DS("pgvector") 失效、chunk 误写 MySQL。
    // 一致性靠"chunk 为衍生数据、重发布幂等重建"兜底（见 publish 内注释）。
    public void delete(Long id) {
        AiKnowledgeDocDO doc = requireDoc(id);
        if (KnowledgeDocStatus.PUBLISHED.equals(doc.getStatus())) {
            offline(id);
            return;
        }
        deleteChunks(id);
        docMapper.deleteById(id);
    }

    @Override
    // 跨库不加事务：publish 混用 MySQL(doc) 与 PG(chunk)，@Transactional 会把连接
    // 钉死在 master 导致 @DS("pgvector") 路由失效（2026-08-21 发布 404/语法错根因）。
    // 中途失败的残留靠"重新发布 = 物理清理重写"自愈，doc 状态以 MySQL 为准。
    public KnowledgePublishResp publish(Long id) {
        AiKnowledgeDocDO doc = requireDoc(id);
        AiEmbeddingConfigDO config = embeddingClient.requireEnabledConfig();
        List<String> chunks = knowledgeChunker.chunk(doc.getContent());
        if (chunks.isEmpty()) {
            throw new BizException(BizCode.PARAM_ERROR, "Knowledge document content cannot be empty");
        }
        // 重建：先物理清理旧分块。chunk 为可重建的衍生数据，清空重写保证幂等，
        // 亦可自愈跨库（PG 写成功而 MySQL 状态回滚）的不一致——重新发布即修复。
        deleteChunks(id);
        int embeddedCount = 0;
        int skippedCount = 0;
        // 分批向量化：把 N 次 HTTP 往返压到 N/EMBED_BATCH_SIZE 次。
        // 配置只取一次贯穿全批——此前每个分块都回查一次库，同样是超时的放大器之一。
        for (int start = 0; start < chunks.size(); start += EMBED_BATCH_SIZE) {
            int end = Math.min(start + EMBED_BATCH_SIZE, chunks.size());
            List<String> batch = chunks.subList(start, end);
            List<List<Double>> vectors = embeddingClient.embedBatch(config, batch);
            for (int i = 0; i < batch.size(); i++) {
                String content = batch.get(i);
                List<Double> embedding = vectors.get(i);
                // 维度自洽校验（第一道）；若与 PG 表 vector(N) 仍不符，由 PG 抛 dimension mismatch（第二道）
                if (config.getDimension() != null && !config.getDimension().equals(embedding.size())) {
                    throw new BizException(BizCode.DOC_PROCESSING_FAILED,
                            "向量维度 " + embedding.size() + " 与配置 " + config.getDimension()
                                    + " 不一致，请核对 embedding 模型或重建");
                }
                AiKnowledgeChunkDO chunk = new AiKnowledgeChunkDO();
                chunk.setId(IdWorker.getId());
                chunk.setDocId(id);
                chunk.setChunkIndex(start + i);
                chunk.setContent(content);
                chunk.setTokenCount(content.length());
                chunk.setEmbeddingModel(config.getModel());
                chunk.setEmbeddingDimension(config.getDimension());
                chunkMapper.insertChunkWithVector(chunk, toJson(embedding));
                embeddedCount++;
            }
        }
        doc.setStatus(KnowledgeDocStatus.PUBLISHED);
        doc.setPublishedAt(LocalDateTime.now());
        docMapper.updateById(doc);
        // 图谱抽取（Neo4j 未启用或不可达时服务内部已降级，不影响文档发布）
        neo4jGraphService.extractAndStore(id, doc.getTitle(), doc.getContent());
        return KnowledgePublishResp.builder()
                .docId(id)
                .chunkCount(chunks.size())
                .embeddedCount(embeddedCount)
                .skippedCount(skippedCount)
                .build();
    }

    @Override
    public void publishAsync(Long id) {
        AiKnowledgeDocDO doc = requireDoc(id);
        doc.setStatus(KnowledgeDocStatus.PROCESSING);
        docMapper.updateById(doc);
        publishExecutor.execute(() -> {
            try {
                publish(id);
            } catch (Exception e) {
                // 调用方此刻早已返回，异常只能落日志；状态回退为草稿，用户可在列表页重新发布
                log.error("文档异步发布失败 docId={}，状态已回退为草稿", id, e);
                AiKnowledgeDocDO failed = requireDoc(id);
                failed.setStatus(KnowledgeDocStatus.DRAFT);
                docMapper.updateById(failed);
            }
        });
    }

    @Override
    // 同 publish：不能加事务（自调用进 publish 也一样会被外层事务钉住连接）
    public KnowledgePublishResp rebuildEmbedding(Long id) {
        return publish(id);
    }

    @Override
    public void offline(Long id) {
        AiKnowledgeDocDO doc = requireDoc(id);
        doc.setStatus(KnowledgeDocStatus.OFFLINE);
        docMapper.updateById(doc);
    }

    private void fill(AiKnowledgeDocDO doc, KnowledgeDocSaveReq req) {
        doc.setTitle(req.getTitle());
        doc.setType(req.getType());
        doc.setContent(req.getContent());
        doc.setSummary(req.getSummary());
    }

    private AiKnowledgeDocDO requireDoc(Long id) {
        AiKnowledgeDocDO doc = docMapper.selectById(id);
        if (doc == null) {
            throw new BizException(BizCode.DOC_NOT_FOUND);
        }
        return doc;
    }

    private void deleteChunks(Long docId) {
        // chunk 表已迁至 PostgreSQL(PgVector)，物理删除（衍生数据，重建幂等）
        chunkMapper.deleteByDocId(docId);
    }

    private String toJson(List<Double> embedding) {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (JsonProcessingException ex) {
            throw new BizException(BizCode.DOC_PROCESSING_FAILED, "序列化向量数据失败");
        }
    }

    private KnowledgeDocResp toResp(AiKnowledgeDocDO doc) {
        return KnowledgeDocResp.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .type(doc.getType())
                .fileUrl(doc.getFileUrl())
                .status(doc.getStatus())
                .content(doc.getContent())
                .summary(doc.getSummary())
                .version(doc.getVersion())
                .publishedAt(doc.getPublishedAt())
                .createTime(doc.getCreateTime())
                .build();
    }
}
