package com.aurora.ai.provider.model.req;

import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class EmbeddingConfigSaveReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String baseUrl;
    private String apiKey;
    private String model;
    private Integer dimension;
    @Min(1)
    private Integer timeoutSeconds = 60;
    private Boolean enabled = Boolean.FALSE;
}