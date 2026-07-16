package com.aurora.ai.knowledge.model.resp;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class KnowledgePublishResp implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long docId;
    private Integer chunkCount;
    private Integer embeddedCount;
    private Integer skippedCount;
}