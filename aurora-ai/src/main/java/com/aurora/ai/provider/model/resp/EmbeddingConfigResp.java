package com.aurora.ai.provider.model.resp;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
public class EmbeddingConfigResp implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String baseUrl;
    private String model;
    private Integer dimension;
    private Integer timeoutSeconds;
    private Boolean enabled;
    private Boolean hasApiKey;
    private LocalDateTime createTime;
}