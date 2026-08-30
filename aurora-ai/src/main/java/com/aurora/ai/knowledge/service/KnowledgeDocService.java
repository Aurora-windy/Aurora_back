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

    /** 同步发布：切分 + 向量化 + 图谱抽取，返回分块统计。耗时随文档长度线性增长 */
    KnowledgePublishResp publish(Long id);

    /**
     * 异步发布：立即把文档置为 PROCESSING 并提交后台任务后返回，不阻塞调用方。
     * 成功则状态落 PUBLISHED，失败回退 DRAFT（可重试）。
     */
    void publishAsync(Long id);

    KnowledgePublishResp rebuildEmbedding(Long id);

    void offline(Long id);
}