package com.aurora.ai.knowledge.service;

import com.aurora.ai.knowledge.model.req.KnowledgeDocPageReq;
import com.aurora.ai.knowledge.model.req.KnowledgeDocSaveReq;
import com.aurora.ai.knowledge.model.resp.KnowledgeDocResp;
import com.aurora.ai.knowledge.model.resp.KnowledgePublishResp;
import com.aurora.common.response.PageResult;

public interface KnowledgeDocService {
    PageResult<KnowledgeDocResp> page(KnowledgeDocPageReq req);

    KnowledgeDocResp detail(Long id);

    Long create(KnowledgeDocSaveReq req);

    void update(Long id, KnowledgeDocSaveReq req);

    /** 记录原文件访问地址（本地文件上传保存后回填） */
    void updateFileUrl(Long id, String fileUrl);

    void delete(Long id);

    KnowledgePublishResp publish(Long id);

    KnowledgePublishResp rebuildEmbedding(Long id);

    void offline(Long id);
}