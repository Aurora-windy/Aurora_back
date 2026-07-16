package com.aurora.ai.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aurora.common.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_knowledge_chunk")
public class AiKnowledgeChunkDO extends BaseDO {
    private Long docId;
    private Integer chunkIndex;
    private String content;
    private Integer tokenCount;
    private String embeddingJson;
    private String embeddingModel;
    private Integer embeddingDimension;
    private String metadataJson;
}