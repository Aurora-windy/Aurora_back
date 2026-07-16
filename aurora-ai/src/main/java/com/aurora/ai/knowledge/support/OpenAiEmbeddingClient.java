package com.aurora.ai.knowledge.support;

import com.aurora.ai.provider.entity.AiEmbeddingConfigDO;
import com.aurora.ai.provider.mapper.AiEmbeddingConfigMapper;
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

    private final AiEmbeddingConfigMapper embeddingConfigMapper;
    private final ObjectMapper objectMapper;

    @Override
    public AiEmbeddingConfigDO requireEnabledConfig() {
        AiEmbeddingConfigDO config = embeddingConfigMapper.selectOne(Wrappers.<AiEmbeddingConfigDO>lambdaQuery()
                .eq(AiEmbeddingConfigDO::getEnabled, 1)
                .orderByDesc(AiEmbeddingConfigDO::getCreateTime)
                .last("LIMIT 1"));
        if (config == null || !StringUtils.hasText(config.getBaseUrl()) || !StringUtils.hasText(config.getModel())
                || config.getDimension() == null || config.getDimension() < 1) {
            throw new BizException(BizCode.LLM_UNAVAILABLE, "发布知识文档前必须先配置 Embedding");
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
            builder.defaultHeader("Authorization", "Bearer " + config.getApiKeyCipher());
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

    private List<Double> parseEmbedding(String response, Integer expectedDimension) {
        try {
            JsonNode embeddingNode = objectMapper.readTree(response).path("data").path(0).path("embedding");
            if (!embeddingNode.isArray()) {
                throw new BizException(BizCode.LLM_UNAVAILABLE, "Embedding 响应中没有向量数据");
            }
            List<Double> vector = new ArrayList<>();
            embeddingNode.forEach(node -> vector.add(node.asDouble()));
            if (expectedDimension != null && expectedDimension > 0 && vector.size() != expectedDimension) {
                throw new BizException(BizCode.LLM_UNAVAILABLE, "Embedding 向量维度不匹配");
            }
            return vector;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(BizCode.LLM_UNAVAILABLE, "解析 Embedding 响应失败");
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.endsWith("/v1") ? normalized : normalized + "/v1";
    }
}
