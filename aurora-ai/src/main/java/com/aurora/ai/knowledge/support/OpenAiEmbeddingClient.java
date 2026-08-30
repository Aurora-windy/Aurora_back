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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
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

    /**
     * 按「baseUrl + 密钥密文 + 超时」缓存 RestClient。
     * <p>背景：文档发布对每个分块都调一次 embedding，此前每次都 new 一个
     * SimpleClientHttpRequestFactory（底层每次新建 TCP + TLS 连接），N 个分块即 N 次握手，
     * 这是上传超时的主放大器。缓存后由 JDK HttpClient 复用 keep-alive 连接。</p>
     * <p>密钥以密文参与缓存键，明文仅存在于 RestClient 的 defaultHeader 中。</p>
     */
    private final Map<String, RestClient> clientCache = new ConcurrentHashMap<>();

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
        return embed(requireEnabledConfig(), text);
    }

    @Override
    public List<Double> embed(AiEmbeddingConfigDO config, String text) {
        AiEmbeddingConfigDO effective = config == null ? requireEnabledConfig() : config;
        String response = client(effective, timeoutOf(effective))
                .post()
                .uri("/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("model", effective.getModel(), "input", text))
                .retrieve()
                .body(String.class);
        JsonNode dataNode = readDataNode(response);
        if (dataNode.isEmpty()) {
            throw new BizException(BizCode.LLM_UNAVAILABLE, "Embedding response does not contain vector data");
        }
        return toVector(dataNode.get(0).path("embedding"), effective.getDimension());
    }

    @Override
    public List<List<Double>> embedBatch(AiEmbeddingConfigDO config, List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        if (texts.size() == 1) {
            return List.of(embed(config, texts.get(0)));
        }
        AiEmbeddingConfigDO effective = config == null ? requireEnabledConfig() : config;
        try {
            String response = client(effective, timeoutOf(effective))
                    .post()
                    .uri("/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("model", effective.getModel(), "input", texts))
                    .retrieve()
                    .body(String.class);
            return parseEmbeddingBatch(readDataNode(response), effective.getDimension(), texts.size());
        } catch (Exception ex) {
            // 部分供应商/中转不接受 input 数组，或批量请求体过大被拒：降级逐条，保证功能可用
            log.warn("批量向量化失败（{} 条），降级为逐条调用：{}", texts.size(), ex.getMessage());
            return embedOneByOne(effective, texts);
        }
    }

    private List<List<Double>> embedOneByOne(AiEmbeddingConfigDO config, List<String> texts) {
        List<List<Double>> vectors = new ArrayList<>(texts.size());
        for (String text : texts) {
            vectors.add(embed(config, text));
        }
        return vectors;
    }

    private Duration timeoutOf(AiEmbeddingConfigDO config) {
        return config.getTimeoutSeconds() == null || config.getTimeoutSeconds() < 1
                ? DEFAULT_TIMEOUT
                : Duration.ofSeconds(config.getTimeoutSeconds());
    }

    /**
     * 取（或建）可复用连接的 RestClient。底层用 JDK HttpClient——自带 keep-alive 连接池，
     * 避免每个分块重复建立 TCP + TLS。
     */
    private RestClient client(AiEmbeddingConfigDO config, Duration timeout) {
        String key = normalizeBaseUrl(config.getBaseUrl()) + "|"
                + (config.getApiKeyCipher() == null ? "" : config.getApiKeyCipher()) + "|"
                + timeout.toMillis();
        return clientCache.computeIfAbsent(key, ignored -> {
            // 强制 HTTP/1.1：JDK HttpClient 默认尝试 HTTP/2，部分中转/网关不支持，
            // 会报 "Received RST_STREAM: Protocol error"（Neo4j 侧已踩过同一个坑）
            HttpClient httpClient = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(timeout)
                    .build();
            JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
            requestFactory.setReadTimeout(timeout);
            RestClient.Builder builder = RestClient.builder()
                    .baseUrl(normalizeBaseUrl(config.getBaseUrl()))
                    .requestFactory(requestFactory);
            if (StringUtils.hasText(config.getApiKeyCipher())) {
                builder.defaultHeader("Authorization", "Bearer " + resolveApiKey(config.getApiKeyCipher()));
            }
            return builder.build();
        });
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
        if (!StringUtils.hasText(provider.getBaseUrl())) {
            throw new BizException(BizCode.LLM_UNAVAILABLE, "Embedding provider configuration is incomplete");
        }
        // embedding 优先用 embedding_model（如 text-embedding-3-small），为空则 fallback 到 model，
        // 兼容个别 chat 模型本身也支持 embedding 的场景。resolveDimension 同样以 embedding 模型名为准，
        // 这样已知 embedding 模型可自动解析维度，无需手动填 dimension。
        String embeddingModel = StringUtils.hasText(provider.getEmbeddingModel())
                ? provider.getEmbeddingModel()
                : provider.getModel();
        if (!StringUtils.hasText(embeddingModel)) {
            throw new BizException(BizCode.LLM_UNAVAILABLE, "Embedding provider configuration is incomplete");
        }
        Integer dimension = resolveDimension(embeddingModel, provider.getEmbeddingDimension());
        AiEmbeddingConfigDO config = new AiEmbeddingConfigDO();
        config.setBaseUrl(provider.getBaseUrl());
        config.setApiKeyCipher(provider.getApiKeyCipher());
        config.setModel(embeddingModel);
        config.setDimension(dimension);
        config.setTimeoutSeconds(provider.getTimeoutSeconds());
        config.setEnabled(provider.getEnabled());
        return config;
    }

    private JsonNode readDataNode(String response) {
        try {
            JsonNode dataNode = objectMapper.readTree(response).path("data");
            if (!dataNode.isArray()) {
                throw new BizException(BizCode.LLM_UNAVAILABLE, "Embedding response does not contain vector data");
            }
            return dataNode;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(BizCode.LLM_UNAVAILABLE, "Failed to parse embedding response");
        }
    }

    private List<Double> toVector(JsonNode embeddingNode, Integer expectedDimension) {
        if (!embeddingNode.isArray()) {
            throw new BizException(BizCode.LLM_UNAVAILABLE, "Embedding response does not contain vector data");
        }
        List<Double> vector = new ArrayList<>();
        embeddingNode.forEach(node -> vector.add(node.asDouble()));
        if (expectedDimension != null && expectedDimension > 0 && vector.size() != expectedDimension) {
            throw new BizException(BizCode.LLM_UNAVAILABLE, "Embedding vector dimension mismatch");
        }
        return vector;
    }

    /** 按返回条目的 index 归位（缺失 index 时按数组顺序兜底），确保向量与入参文本一一对应 */
    private List<List<Double>> parseEmbeddingBatch(JsonNode dataNode, Integer expectedDimension, int expectedCount) {
        List<List<Double>> slots = new ArrayList<>(Collections.nCopies(expectedCount, null));
        int cursor = 0;
        for (JsonNode node : dataNode) {
            List<Double> vector = toVector(node.path("embedding"), expectedDimension);
            int idx = node.hasNonNull("index") ? node.path("index").asInt() : cursor;
            if (idx >= 0 && idx < expectedCount) {
                slots.set(idx, vector);
            }
            cursor++;
        }
        for (int i = 0; i < expectedCount; i++) {
            if (slots.get(i) == null) {
                throw new BizException(BizCode.LLM_UNAVAILABLE, "批量向量化返回条数不符，期望 " + expectedCount + " 条");
            }
        }
        return slots;
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
