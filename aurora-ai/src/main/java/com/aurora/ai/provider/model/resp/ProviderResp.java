
package com.aurora.ai.provider.model.resp;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProviderResp implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String code;
    private String name;
    private String model;
    private String usageType;
    private Integer embeddingDimension;
    private BigDecimal temperature;
    private Integer maxTokens;
    private Integer timeoutSeconds;
    private Boolean enabled;
    private Integer sortOrder;
    private Boolean hasApiKey;
    private LocalDateTime createTime;
}
