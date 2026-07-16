package com.aurora.ai.knowledge.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.ai.knowledge.model.req.KnowledgeDocPageReq;
import com.aurora.ai.knowledge.model.req.KnowledgeDocSaveReq;
import com.aurora.ai.knowledge.model.req.KnowledgeSearchReq;
import com.aurora.ai.knowledge.model.resp.KnowledgeCitation;
import com.aurora.ai.knowledge.model.resp.KnowledgeDocResp;
import com.aurora.ai.knowledge.model.resp.KnowledgePublishResp;
import com.aurora.ai.knowledge.service.KnowledgeDocService;
import com.aurora.ai.knowledge.service.KnowledgeRetrievalService;
import com.aurora.common.constant.PermCodeConst;
import com.aurora.common.response.PageResult;
import com.aurora.common.response.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/admin/knowledge-docs")
public class AiKnowledgeDocController {

    private final KnowledgeDocService knowledgeDocService;
    private final KnowledgeRetrievalService knowledgeRetrievalService;

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