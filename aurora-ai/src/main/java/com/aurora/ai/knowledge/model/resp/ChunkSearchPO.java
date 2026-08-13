package com.aurora.ai.knowledge.model.resp;

import lombok.Data;

/**
 * 向量检索原生结果（PostgreSQL 侧），供 KnowledgeRetrievalService 回填文档信息后组装引用。
 */
@Data
public class ChunkSearchPO {

    private Long id;

    private Long docId;

    private Integer chunkIndex;

    private String content;

    private String embeddingModel;

    /** 相似度得分 = 1 - 余弦距离，越大越相似 */
    private Double score;
}
