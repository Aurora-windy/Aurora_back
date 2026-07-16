package com.aurora.ai.knowledge.model.resp;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class KnowledgeCitation implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long docId;
    private String docTitle;
    private String docType;
    private Long chunkId;
    private String snippet;
    private Double score;
}