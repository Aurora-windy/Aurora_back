package com.aurora.ai.provider.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aurora.ai.embedding")
public class SystemEmbeddingProperties {
    private Boolean enabled = Boolean.FALSE;
    private String providerMode = "local";
    private Boolean cloudConsent = Boolean.FALSE;
    private String baseUrl;
    private String apiKey;
    private String model;
    private Integer dimension;
    private Integer timeoutSeconds = 60;
}
