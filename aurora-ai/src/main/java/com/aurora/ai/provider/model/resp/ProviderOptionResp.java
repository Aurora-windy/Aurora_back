package com.aurora.ai.provider.model.resp;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
public class ProviderOptionResp implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String code;
    private String name;
    private String baseUrl;
    private String model;
    private BigDecimal temperature;
    private Integer maxTokens;
    private Integer timeoutSeconds;
    private Integer sortOrder;
}