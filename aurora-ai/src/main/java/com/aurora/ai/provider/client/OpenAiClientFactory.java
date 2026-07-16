package com.aurora.ai.provider.client;

import com.aurora.ai.provider.entity.AiModelProviderDO;
import com.aurora.ai.provider.entity.AiEmbeddingConfigDO;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiClientFactory {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    public void testChatCompletion(AiModelProviderDO provider) {
        Duration timeout = provider.getTimeoutSeconds() == null || provider.getTimeoutSeconds() < 1
                ? DEFAULT_TIMEOUT
                : Duration.ofSeconds(provider.getTimeoutSeconds());
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(normalizeBaseUrl(provider.getBaseUrl()))
                .requestFactory(requestFactory);
        if (StringUtils.hasText(provider.getApiKeyCipher())) {
            builder.defaultHeader("Authorization", "Bearer " + provider.getApiKeyCipher());
        }

        Map<String, Object> body = Map.of(
                "model", provider.getModel(),
                "messages", List.of(Map.of("role", "user", "content", "ping")),
                "temperature", 0,
                "max_tokens", 1
        );

        builder.build()
                .post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    public void testEmbedding(AiEmbeddingConfigDO config) {
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

        Map<String, Object> body = Map.of(
                "model", config.getModel(),
                "input", "ping"
        );

        builder.build()
                .post()
                .uri("/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.endsWith("/v1") ? normalized : normalized + "/v1";
    }
}
