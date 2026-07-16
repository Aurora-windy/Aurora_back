package com.aurora.ai.provider.model.req;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class ProviderSaveReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    private String code;
    @NotBlank
    private String name;
    @NotBlank
    private String baseUrl;
    private String apiKey;
    @NotBlank
    private String model;
    @DecimalMin("0.00")
    @DecimalMax("2.00")
    private BigDecimal temperature = new BigDecimal("0.70");
    @Min(1)
    private Integer maxTokens;
    @Min(1)
    private Integer timeoutSeconds = 60;
    private Boolean enabled = Boolean.TRUE;
    private Integer sortOrder = 0;
}