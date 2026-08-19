package com.aurora.ai.provider.client;

import com.aurora.ai.provider.entity.AiModelProviderDO;
import com.aurora.ai.provider.entity.AiEmbeddingConfigDO;
import com.aurora.ai.provider.support.AiSecretCipher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class OpenAiClientFactory {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    private final ObjectMapper objectMapper;

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
            builder.defaultHeader("Authorization", "Bearer " + AiSecretCipher.decrypt(provider.getApiKeyCipher()));
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

    public void testEmbedding(AiModelProviderDO provider) {
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
            builder.defaultHeader("Authorization", "Bearer " + AiSecretCipher.decrypt(provider.getApiKeyCipher()));
        }

        Map<String, Object> body = Map.of(
                "model", StringUtils.hasText(provider.getEmbeddingModel())
                        ? provider.getEmbeddingModel()
                        : provider.getModel(),
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
            builder.defaultHeader("Authorization", "Bearer " + AiSecretCipher.decrypt(config.getApiKeyCipher()));
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

    public String chatCompletion(AiModelProviderDO provider, List<Map<String, String>> messages, Double temperature, Integer maxTokens) {
        return chatCompletionResult(provider, messages, temperature, maxTokens).content();
    }

    /**
     * 与 {@link #chatCompletion} 相同，但额外返回 token 用量。
     */
    public ChatResult chatCompletionResult(AiModelProviderDO provider, List<Map<String, String>> messages, Double temperature, Integer maxTokens) {
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
            builder.defaultHeader("Authorization", "Bearer " + AiSecretCipher.decrypt(provider.getApiKeyCipher()));
        }

        List<Map<String, Object>> bodyMessages = new ArrayList<>();
        for (Map<String, String> message : messages) {
            bodyMessages.add(Map.of("role", message.get("role"), "content", message.get("content")));
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", provider.getModel());
        body.put("messages", bodyMessages);
        if (temperature != null) {
            body.put("temperature", temperature);
        }
        if (maxTokens != null && maxTokens > 0) {
            body.put("max_tokens", maxTokens);
        }

        JsonNode response = builder.build()
                .post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        if (response == null || response.path("choices").isEmpty()) {
            throw new RuntimeException("模型返回为空");
        }
        String content = response.path("choices").get(0).path("message").path("content").asText("");
        if (!StringUtils.hasText(content)) {
            throw new RuntimeException("模型返回内容为空");
        }
        JsonNode usage = response.path("usage");
        Integer promptTokens = usage.path("prompt_tokens").asInt(0);
        Integer completionTokens = usage.path("completion_tokens").asInt(0);
        Integer totalTokens = usage.path("total_tokens").asInt(0);
        return new ChatResult(content, promptTokens, completionTokens, totalTokens);
    }

    /** 模型调用结果：回复内容 + token 用量 */
    public record ChatResult(String content, Integer promptTokens, Integer completionTokens, Integer totalTokens) {
    }

    /**
     * 流式对话补全：逐 token 回调 {@code onToken}，最终用量回调 {@code onUsage}。
     * 通过 RestClient 的 exchange 读取上游 SSE 响应流（{@code data: {...}} 行），
     * 兼容 OpenAI 在最后一个 chunk（需 {@code stream_options.include_usage=true}）回传 usage 的约定。
     */
    public void streamChatCompletion(AiModelProviderDO provider, List<Map<String, String>> messages,
                                     Double temperature, Integer maxTokens,
                                     Consumer<String> onToken, Consumer<StreamUsage> onUsage) {
        Duration timeout = provider.getTimeoutSeconds() == null || provider.getTimeoutSeconds() < 1
                ? DEFAULT_TIMEOUT : Duration.ofSeconds(provider.getTimeoutSeconds());
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(normalizeBaseUrl(provider.getBaseUrl()))
                .requestFactory(requestFactory);
        if (StringUtils.hasText(provider.getApiKeyCipher())) {
            builder.defaultHeader("Authorization", "Bearer " + AiSecretCipher.decrypt(provider.getApiKeyCipher()));
        }

        List<Map<String, Object>> bodyMessages = new ArrayList<>();
        for (Map<String, String> message : messages) {
            bodyMessages.add(Map.of("role", message.get("role"), "content", message.get("content")));
        }
        Map<String, Object> body = new HashMap<>();
        body.put("model", provider.getModel());
        body.put("messages", bodyMessages);
        body.put("stream", true);
        body.put("stream_options", Map.of("include_usage", true));
        if (temperature != null) {
            body.put("temperature", temperature);
        }
        if (maxTokens != null && maxTokens > 0) {
            body.put("max_tokens", maxTokens);
        }

        builder.build().post().uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .body(body)
                .exchange((request, response) -> {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                        String line;
                        boolean errorEvent = false;
                        while ((line = reader.readLine()) != null) {
                            // 识别 SSE event: error（部分中转站在上游故障时发 error 事件而非 data）
                            if (line.startsWith("event:")) {
                                String eventName = line.substring(6).trim();
                                errorEvent = "error".equalsIgnoreCase(eventName);
                                continue;
                            }
                            if (!line.startsWith("data:")) {
                                continue;
                            }
                            String data = line.substring(5).trim();
                            if (errorEvent && !data.isEmpty() && !"[DONE]".equals(data)) {
                                // 上游明确报错：解析 error JSON 并抛出，避免静默吞掉导致前端"无返回"
                                throw new RuntimeException("上游错误: " + extractErrorMessage(data));
                            }
                            errorEvent = false;
                            if (data.isEmpty() || "[DONE]".equals(data)) {
                                continue;
                            }
                            JsonNode node = objectMapper.readTree(data);
                            JsonNode choices = node.path("choices");
                            if (choices.isArray() && !choices.isEmpty()) {
                                String delta = choices.get(0).path("delta").path("content").asText("");
                                if (!delta.isEmpty() && onToken != null) {
                                    onToken.accept(delta);
                                }
                            }
                            JsonNode usage = node.path("usage");
                            if (!usage.isMissingNode() && !usage.isNull() && onUsage != null) {
                                onUsage.accept(new StreamUsage(
                                        usage.path("prompt_tokens").asInt(0),
                                        usage.path("completion_tokens").asInt(0),
                                        usage.path("total_tokens").asInt(0)));
                            }
                        }
                    } catch (Exception ex) {
                        throw new RuntimeException("读取流式响应失败: " + ex.getMessage(), ex);
                    }
                    return null;
                });
    }

    /** 流式用量快照 */
    public record StreamUsage(Integer promptTokens, Integer completionTokens, Integer totalTokens) {
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.endsWith("/v1") ? normalized : normalized + "/v1";
    }

    /** 从上游错误事件 JSON 中提取可读错误信息（{@code {"error":{"message":...}}}） */
    private String extractErrorMessage(String data) {
        try {
            JsonNode node = objectMapper.readTree(data);
            JsonNode err = node.path("error");
            if (!err.isMissingNode() && !err.isNull()) {
                String msg = err.path("message").asText("");
                return msg.isEmpty() ? data : msg;
            }
            return data;
        } catch (Exception e) {
            return data;
        }
    }
}
