package com.aurora.ai.provider.model.req;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class ProviderSaveReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // code 选填：为空时由后端自动生成（prov_ + 12位随机串），见 AiProviderServiceImpl.generateCode
    private String code;
    @NotBlank
    private String name;
    private String baseUrl;
    private String apiKey;
    @NotBlank
    private String model;
    private String embeddingModel;
    @Pattern(regexp = "CHAT|EMBEDDING|BOTH|GRAPH")
    private String usageType = "CHAT";
    @Min(1)
    private Integer embeddingDimension;
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
