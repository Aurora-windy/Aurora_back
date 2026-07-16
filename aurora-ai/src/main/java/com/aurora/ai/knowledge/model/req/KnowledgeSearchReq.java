package com.aurora.ai.knowledge.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class KnowledgeSearchReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    private String query;
    @Min(1)
    private Integer topK = 5;
}