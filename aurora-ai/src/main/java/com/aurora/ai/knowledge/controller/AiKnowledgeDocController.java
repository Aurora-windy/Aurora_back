package com.aurora.ai.knowledge.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.ai.knowledge.model.req.KnowledgeDocPageReq;
import com.aurora.ai.knowledge.model.req.KnowledgeDocSaveReq;
import com.aurora.ai.knowledge.model.req.KnowledgeSearchReq;
import com.aurora.ai.knowledge.model.resp.KnowledgeCitation;
import com.aurora.ai.knowledge.model.resp.KnowledgeDocResp;
import com.aurora.ai.knowledge.model.resp.KnowledgePublishResp;
import com.aurora.ai.knowledge.model.resp.KnowledgeUploadResp;
import com.aurora.ai.knowledge.service.KnowledgeDocService;
import com.aurora.ai.knowledge.service.KnowledgeRetrievalService;
import com.aurora.ai.knowledge.support.DocTextExtractor;
import com.aurora.ai.knowledge.support.LocalFileStore;
import com.aurora.common.constant.PermCodeConst;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.aurora.common.response.PageResult;
import com.aurora.common.response.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/admin/knowledge-docs")
public class AiKnowledgeDocController {

    private final KnowledgeDocService knowledgeDocService;
    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final LocalFileStore localFileStore;

    private static final long MAX_FILE_BYTES = 5 * 1024 * 1024L; // 5MB

    /**
     * 上传文档文件（txt/md/csv/docx/xlsx）：先保存原文件到本地（返回可访问 URL），
     * 再提取文本 → 保存草稿 → 尝试自动发布。
     * embedding 未配置或发布失败时降级为草稿，可在列表页手动重试发布。
     */
    @SaCheckPermission(PermCodeConst.Ai.Knowledge.CREATE)
    @PostMapping("/upload")
    public Result<KnowledgeUploadResp> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(BizCode.PARAM_ERROR, "上传文件不能为空");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BizException(BizCode.PARAM_ERROR, "文件过大，请上传 5MB 以内的文件");
        }
        String fileName = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename() : "unnamed.txt";
        if (!DocTextExtractor.supports(fileName)) {
            throw new BizException(BizCode.PARAM_ERROR,
                    "不支持的文件类型：" + fileName + "（支持 txt / md / csv / docx / xlsx）");
        }
        // 1. 保存原文件到本地（参考 ContiNew Admin 本地存储方案），返回可访问 URL
        LocalFileStore.FileRef fileRef = localFileStore.store(file);
        // 2. 提取文本
        String content;
        try {
            content = DocTextExtractor.extract(fileName, file.getBytes());
        } catch (IllegalArgumentException e) {
            throw new BizException(BizCode.PARAM_ERROR, e.getMessage());
        } catch (Exception e) {
            throw new BizException(BizCode.DOC_PROCESSING_FAILED, "文件解析失败：" + e.getMessage());
        }
        if (!StringUtils.hasText(content.trim())) {
            throw new BizException(BizCode.PARAM_ERROR, "文件内容为空，无法入库");
        }
        // 3. 保存为知识文档草稿（记录原文件地址）
        KnowledgeDocSaveReq req = new KnowledgeDocSaveReq();
        req.setTitle(fileName);
        req.setType("UPLOAD");
        req.setContent(content);
        Long docId = knowledgeDocService.create(req);
        knowledgeDocService.updateFileUrl(docId, fileRef.url());
        // 4. 尝试自动发布（切分 + embedding + 图谱抽取）；embedding 未配置时降级为草稿
        boolean published = false;
        String message = "上传成功，已保存为草稿";
        try {
            knowledgeDocService.publish(docId);
            published = true;
            message = "上传成功，已自动发布";
        } catch (BizException e) {
            message = "上传成功，但自动发布失败：" + e.getMessage() + "（可在列表手动重试发布）";
        } catch (Exception e) {
            message = "上传成功，但自动发布失败：" + e.getMessage();
        }
        return Result.ok(new KnowledgeUploadResp(docId, fileName, fileRef.url(), content.length(), published, message));
    }

    @SaCheckPermission(PermCodeConst.Ai.Knowledge.LIST)
    @GetMapping
    public Result<PageResult<KnowledgeDocResp>> page(KnowledgeDocPageReq req) {
        return Result.ok(knowledgeDocService.page(req));
    }

    @SaCheckPermission(PermCodeConst.Ai.Knowledge.LIST)
    @GetMapping("/{id}")
    public Result<KnowledgeDocResp> detail(@PathVariable Long id) {
        return Result.ok(knowledgeDocService.detail(id));
    }

    @SaCheckPermission(PermCodeConst.Ai.Knowledge.CREATE)
    @PostMapping
    public Result<Long> create(@RequestBody @Valid KnowledgeDocSaveReq req) {
        return Result.ok(knowledgeDocService.create(req));
    }

    @SaCheckPermission(PermCodeConst.Ai.Knowledge.UPDATE)
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody @Valid KnowledgeDocSaveReq req) {
        knowledgeDocService.update(id, req);
        return Result.ok(Boolean.TRUE);
    }

    @SaCheckPermission(PermCodeConst.Ai.Knowledge.DELETE)
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        knowledgeDocService.delete(id);
        return Result.ok(Boolean.TRUE);
    }

    @SaCheckPermission(PermCodeConst.Ai.Knowledge.PUBLISH)
    @PostMapping("/{id}/publish")
    public Result<KnowledgePublishResp> publish(@PathVariable Long id) {
        return Result.ok(knowledgeDocService.publish(id));
    }

    @SaCheckPermission(PermCodeConst.Ai.Knowledge.PUBLISH)
    @PostMapping("/{id}/rebuild-embedding")
    public Result<KnowledgePublishResp> rebuildEmbedding(@PathVariable Long id) {
        return Result.ok(knowledgeDocService.rebuildEmbedding(id));
    }

    @SaCheckPermission(PermCodeConst.Ai.Knowledge.PUBLISH)
    @PostMapping("/{id}/offline")
    public Result<Boolean> offline(@PathVariable Long id) {
        knowledgeDocService.offline(id);
        return Result.ok(Boolean.TRUE);
    }

    @SaCheckPermission(PermCodeConst.Ai.Knowledge.LIST)
    @PostMapping("/search")
    public Result<List<KnowledgeCitation>> search(@RequestBody @Valid KnowledgeSearchReq req) {
        return Result.ok(knowledgeRetrievalService.search(req.getQuery(), req.getTopK() == null ? 5 : req.getTopK()));
    }
}