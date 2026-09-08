package com.aurora.ai.knowledge.support;

public final class KnowledgeDocStatus {
    public static final String DRAFT = "DRAFT";

    /**
     * 异步发布进行中。上传接口不再同步等待向量化，而是置为该状态后立即返回，
     * 由前端轮询详情接口直到状态落到 PUBLISHED（成功）或 DRAFT（失败可重试）。
     */
    public static final String PROCESSING = "PROCESSING";

    /** 异步发布队列拒绝或执行失败时的明确终态。 */
    public static final String FAILED = "FAILED";

    public static final String PUBLISHED = "PUBLISHED";
    public static final String OFFLINE = "OFFLINE";

    private KnowledgeDocStatus() {
    }
}
