package com.aurora.ai.knowledge.support;

import com.aurora.ai.provider.config.SystemEmbeddingProperties;
import com.aurora.ai.provider.entity.AiEmbeddingConfigDO;
import com.aurora.ai.provider.entity.AiModelProviderDO;
import com.aurora.ai.provider.mapper.AiEmbeddingConfigMapper;
import com.aurora.ai.provider.mapper.AiModelProviderMapper;
import com.aurora.ai.provider.support.AiSecretCipher;
import com.aurora.ai.provider.support.EmbeddingDimensionResolver;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiEmbeddingClient implements EmbeddingClient {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);
    private static final String USAGE_EMBEDDING = "EMBEDDING";
    private static final String USAGE_BOTH = "BOTH";

    private final SystemEmbeddingProperties systemEmbeddingProperties;
    private final AiEmbeddingConfigMapper embeddingConfigMapper;
    private final AiModelProviderMapper providerMapper;
    private final ObjectMapper objectMapper;

    @Override
    public AiEmbeddingConfigDO requireEnabledConfig() {
        AiEmbeddingConfigDO systemConfig = toSystemEmbeddingConfig();
        if (systemConfig != null) {
            return systemConfig;
        }

        AiModelProviderDO provider = providerMapper.selectOne(Wrappers.<AiModelProviderDO>lambdaQuery()
                .eq(AiModelProviderDO::getEnabled, 1)
                .in(AiModelProviderDO::getUsageType, USAGE_EMBEDDING, USAGE_BOTH)
                .orderByAsc(AiModelProviderDO::getSortOrder)
                .orderByDesc(AiModelProviderDO::getCreateTime)
                .last("LIMIT 1"));
        if (provider != null) {
            return toEmbeddingConfig(provider);
        }

        AiEmbeddingConfigDO config = embeddingConfigMapper.selectOne(Wrappers.<AiEmbeddingConfigDO>lambdaQuery()
                .eq(AiEmbeddingConfigDO::getEnabled, 1)
                .orderByDesc(AiEmbeddingConfigDO::getCreateTime)
                .last("LIMIT 1"));
        if (config == null || !StringUtils.hasText(config.getBaseUrl()) || !StringUtils.hasText(config.getModel())
                || config.getDimension() == null || config.getDimension() < 1) {
            throw new BizException(BizCode.LLM_UNAVAILABLE, "Please configure an embedding-capable model provider before publishing knowledge documents");
        }
        return config;
    }

    @Override
    public List<Double> embed(String text) {
        AiEmbeddingConfigDO config = requireEnabledConfig();
        Duration timeout = config.getTimeoutSeconds() == null || config.getTimeoutSeconds() < 1
                ? DEFAULT_TIMEOUT
                : Duration.ofSeconds(config.getTimeoutSeconds());
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(normalizeBaseUrl(config.getBaseUrl()))
                .requestFactory(requestFactory);
        if (StringUtils.hasText(config.getApiKeyCipher())) {
            builder.defaultHeader("Authorization", "Bearer " + resolveApiKey(config.getApiKeyCipher()));
        }
        Map<String, Object> body = Map.of("model", config.getModel(), "input", text);
        String response = builder.build()
                .post()
                .uri("/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
        return parseEmbedding(response, config.getDimension());
    }

    private AiEmbeddingConfigDO toSystemEmbeddingConfig() {
        if (!Boolean.TRUE.equals(systemEmbeddingProperties.getEnabled()) && !hasSystemEmbeddingTriplet()) {
            return null;
        }
        ensureCloudConsent();
        if (!StringUtils.hasText(systemEmbeddingProperties.getBaseUrl())
                || !StringUtils.hasText(systemEmbeddingProperties.getModel())) {
            throw new BizException(BizCode.LLM_UNAVAILABLE, "System embedding configuration is incomplete");
        }
        Integer dimension = resolveDimension(systemEmbeddingProperties.getModel(), systemEmbeddingProperties.getDimension());
        AiEmbeddingConfigDO config = new AiEmbeddingConfigDO();
        config.setBaseUrl(systemEmbeddingProperties.getBaseUrl());
        config.setApiKeyCipher(systemEmbeddingProperties.getApiKey());
        config.setModel(systemEmbeddingProperties.getModel());
        config.setDimension(dimension);
        config.setTimeoutSeconds(systemEmbeddingProperties.getTimeoutSeconds());
        config.setEnabled(1);
        return config;
    }

    private void ensureCloudConsent() {
        if ("cloud".equalsIgnoreCase(systemEmbeddingProperties.getProviderMode())
                && !Boolean.TRUE.equals(systemEmbeddingProperties.getCloudConsent())) {
            throw new BizException(BizCode.LLM_UNAVAILABLE, "Cloud embedding is disabled until explicit backend consent is enabled");
        }
    }

    private boolean hasSystemEmbeddingTriplet() {
        return StringUtils.hasText(systemEmbeddingProperties.getBaseUrl())
                && StringUtils.hasText(systemEmbeddingProperties.getApiKey())
                && StringUtils.hasText(systemEmbeddingProperties.getModel());
    }

    private AiEmbeddingConfigDO toEmbeddingConfig(AiModelProviderDO provider) {
        if (!StringUtils.hasText(provider.getBaseUrl()) || !StringUtils.hasText(provider.getModel())) {
            throw new BizException(BizCode.LLM_UNAVAILABLE, "Embedding provider configuration is incomplete");
        }
        Integer dimension = resolveDimension(provider.getModel(), provider.getEmbeddingDimension());
        AiEmbeddingConfigDO config = new AiEmbeddingConfigDO();
        config.setBaseUrl(provider.getBaseUrl());
        config.setApiKeyCipher(provider.getApiKeyCipher());
        config.setModel(provider.getModel());
        config.setDimension(dimension);
        config.setTimeoutSeconds(provider.getTimeoutSeconds());
        config.setEnabled(provider.getEnabled());
        return config;
    }

    private List<Double> parseEmbedding(String response, Integer expectedDimension) {
        try {
            JsonNode embeddingNode = objectMapper.readTree(response).path("data").path(0).path("embedding");
            if (!embeddingNode.isArray()) {
                throw new BizException(BizCode.LLM_UNAVAILABLE, "Embedding response does not contain vector data");
            }
            List<Double> vector = new ArrayList<>();
            embeddingNode.forEach(node -> vector.add(node.asDouble()));
            if (expectedDimension != null && expectedDimension > 0 && vector.size() != expectedDimension) {
                throw new BizException(BizCode.LLM_UNAVAILABLE, "Embedding vector dimension mismatch");
            }
            return vector;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(BizCode.LLM_UNAVAILABLE, "Failed to parse embedding response");
        }
    }

    private Integer resolveDimension(String model, Integer configuredDimension) {
        Integer dimension = EmbeddingDimensionResolver.resolve(model, configuredDimension);
        if (dimension == null || dimension < 1) {
            throw new BizException(BizCode.LLM_UNAVAILABLE, "Embedding model dimension is unknown. Use a known embedding model or configure dimension on the backend.");
        }
        return dimension;
    }

    private String resolveApiKey(String apiKeyOrCipher) {
        if (!StringUtils.hasText(apiKeyOrCipher)) {
            return apiKeyOrCipher;
        }
        return apiKeyOrCipher.startsWith("enc:v1:") ? AiSecretCipher.decrypt(apiKeyOrCipher) : apiKeyOrCipher;
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.endsWith("/v1") ? normalized : normalized + "/v1";
    }
}
