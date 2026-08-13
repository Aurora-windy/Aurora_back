package com.aurora.ai.provider.model.resp;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Embedding 模型选项（前端下拉用）。维度由模型决定，管理员无需手填。
 */
@Data
@Builder
public class EmbeddingModelOptionResp implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String model;
    private Integer dimension;
    private String source;   // LOCAL | CLOUD
    private String hint;
}
