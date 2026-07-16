package com.aurora.ai.knowledge.model.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class KnowledgeDocSaveReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    private String title;
    @NotBlank
    private String type;
    @NotBlank
    private String content;
    private String summary;
}