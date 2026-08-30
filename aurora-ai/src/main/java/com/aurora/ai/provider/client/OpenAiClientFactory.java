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
import org.springframework.web.client.RestClientResponseException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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

    public String chatCompletion(AiModelProviderDO provider, List<Map<String, Object>> messages, Double temperature, Integer maxTokens) {
        return chatCompletionResult(provider, messages, temperature, maxTokens, null).content();
    }

    /**
     * 与 {@link #chatCompletion} 相同，但额外返回 token 用量。
     */
    public ChatResult chatCompletionResult(AiModelProviderDO provider, List<Map<String, Object>> messages, Double temperature, Integer maxTokens) {
        return chatCompletionResult(provider, messages, temperature, maxTokens, null);
    }

    /**
     * 带工具（Function Calling）的非流式对话补全。
     * messages 泛型放宽为 Object 以承载 FC 协议消息（assistant 携 tool_calls、tool 携 tool_call_id）；
     * tools 为空时行为与无工具重载完全一致。
     */
    public ChatResult chatCompletionResult(AiModelProviderDO provider, List<Map<String, Object>> messages,
                                           Double temperature, Integer maxTokens, List<Map<String, Object>> tools) {
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
        for (Map<String, Object> message : messages) {
            bodyMessages.add(new LinkedHashMap<>(message));
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
        if (tools != null && !tools.isEmpty()) {
            body.put("tools", tools);
            body.put("tool_choice", "auto");
        }

        JsonNode response;
        try {
            // 先按 String 接收再自行解析：部分供应商/中转返回的 Content-Type 是
            // application/octet-stream 而非 application/json，直接取 JsonNode 会因
            // 找不到匹配的消息转换器而抛 "Error while extracting response"。
            String raw = builder.build()
                    .post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            response = objectMapper.readTree(raw);
        } catch (RestClientResponseException ex) {
            // 4xx/5xx 的响应体里才有真正的失败原因（InvalidApiKey / ModelNotSupported / 余额不足…），
            // 而默认的异常信息是 "401 Unauthorized: [no body]"，把原因吃光了，排障时等于瞎猜。
            String raw = ex.getResponseBodyAsString();
            throw new RuntimeException("模型调用失败 HTTP " + ex.getStatusCode().value()
                    + "，供应商响应体=" + (StringUtils.hasText(raw) ? raw : "（空，供应商未返回错误详情）"));
        } catch (Exception ex) {
            throw new RuntimeException("模型响应解析失败（供应商返回的不是合法 JSON）: " + ex.getMessage());
        }
        if (response == null || response.path("choices").isEmpty()) {
            throw new RuntimeException("模型返回为空");
        }
        JsonNode message = response.path("choices").get(0).path("message");
        String content = message.path("content").asText("");
        // 推理类模型（R1 / o1 系，部分中转以 gpt-5.x 等名义提供）会把正文放进 reasoning_content
        // 而 content 为空。只读 content 时表现为「调用没报错但结果为空」，图谱抽取尤其容易踩到。
        if (!StringUtils.hasText(content)) {
            content = message.path("reasoning_content").asText("");
        }
        List<ToolCall> toolCalls = parseToolCalls(message.path("tool_calls"));
        // FC 场景下模型可只回 tool_calls 而 content 为空，属正常；两者皆空才是真异常
        if (!StringUtils.hasText(content) && toolCalls.isEmpty()) {
            throw new RuntimeException("模型返回内容为空");
        }
        JsonNode usage = response.path("usage");
        Integer promptTokens = usage.path("prompt_tokens").asInt(0);
        Integer completionTokens = usage.path("completion_tokens").asInt(0);
        Integer totalTokens = usage.path("total_tokens").asInt(0);
        return new ChatResult(content, promptTokens, completionTokens, totalTokens, toolCalls);
    }

    /** 模型调用结果：回复内容 + token 用量 + FC 工具调用请求（无工具调用时为空列表） */
    public record ChatResult(String content, Integer promptTokens, Integer completionTokens, Integer totalTokens,
                             List<ToolCall> toolCalls) {
    }

    /** OpenAI FC 协议的一次工具调用请求（arguments 为模型产出的 JSON 字符串，合法性由上层容错解析） */
    public record ToolCall(String id, String name, String argumentsJson) {
    }

    private List<ToolCall> parseToolCalls(JsonNode toolCallsNode) {
        if (toolCallsNode == null || !toolCallsNode.isArray() || toolCallsNode.isEmpty()) {
            return List.of();
        }
        List<ToolCall> calls = new ArrayList<>();
        for (JsonNode node : toolCallsNode) {
            String name = node.path("function").path("name").asText("");
            if (name.isEmpty()) {
                continue;
            }
            String id = node.path("id").asText("");
            String arguments = node.path("function").path("arguments").asText("");
            calls.add(new ToolCall(id, name, arguments.isEmpty() ? "{}" : arguments));
        }
        return calls;
    }

    /**
     * 流式对话补全：逐 token 回调 {@code onToken}，最终用量回调 {@code onUsage}。
     * 通过 RestClient 的 exchange 读取上游 SSE 响应流（{@code data: {...}} 行），
     * 兼容 OpenAI 在最后一个 chunk（需 {@code stream_options.include_usage=true}）回传 usage 的约定。
     */
    public void streamChatCompletion(AiModelProviderDO provider, List<Map<String, Object>> messages,
                                     Double temperature, Integer maxTokens,
                                     Consumer<String> onToken, Consumer<StreamUsage> onUsage) {
        streamChatCompletion(provider, messages, temperature, maxTokens, null, onToken, onUsage, null);
    }

    /**
     * 带工具（FC）的流式对话补全：content 增量仍走 onToken；tool_calls 以碎片到达
     * （{@code delta.tool_calls[i].{index,id,function:{name,arguments 片段}}}），按 index 聚合拼接
     * arguments，{@code finish_reason=tool_calls}（或流结束兜底）时一次性回调 onToolCalls。
     * 降级链（T1 §3.4）：流式解析异常且尚未发出任何 token 时，自动改走非流式重试一次；
     * 已发出 token 则直接抛出，避免非流式重试造成内容重复。
     */
    public void streamChatCompletion(AiModelProviderDO provider, List<Map<String, Object>> messages,
                                     Double temperature, Integer maxTokens, List<Map<String, Object>> tools,
                                     Consumer<String> onToken, Consumer<StreamUsage> onUsage,
                                     Consumer<List<ToolCall>> onToolCalls) {
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
        for (Map<String, Object> message : messages) {
            bodyMessages.add(new LinkedHashMap<>(message));
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
        if (tools != null && !tools.isEmpty()) {
            body.put("tools", tools);
            body.put("tool_choice", "auto");
        }

        final ToolCallAggregator aggregator = new ToolCallAggregator();
        try {
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
                                    JsonNode deltaNode = choices.get(0).path("delta");
                                    String delta = deltaNode.path("content").asText("");
                                    if (!delta.isEmpty() && onToken != null) {
                                        aggregator.tokensEmitted = true;
                                        onToken.accept(delta);
                                    }
                                    JsonNode toolCallDeltas = deltaNode.path("tool_calls");
                                    if (toolCallDeltas.isArray()) {
                                        aggregator.merge(toolCallDeltas);
                                    }
                                    String finishReason = choices.get(0).path("finish_reason").asText("");
                                    if ("tool_calls".equals(finishReason)) {
                                        aggregator.fireOnce(onToolCalls);
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
            // 部分供应商不发 finish_reason=tool_calls：流正常结束但有聚合数据时兜底回调
            aggregator.fireOnce(onToolCalls);
        } catch (Exception ex) {
            if (tools == null || tools.isEmpty() || aggregator.tokensEmitted) {
                throw ex instanceof RuntimeException ? (RuntimeException) ex : new RuntimeException(ex);
            }
            // 降级链：流式失败且零 token 已发出 → 非流式重试一次，结果经原回调通道返回
            ChatResult fallback = chatCompletionResult(provider, messages, temperature, maxTokens, tools);
            if (StringUtils.hasText(fallback.content()) && onToken != null) {
                aggregator.tokensEmitted = true;
                onToken.accept(fallback.content());
            }
            if (onUsage != null) {
                onUsage.accept(new StreamUsage(fallback.promptTokens(), fallback.completionTokens(), fallback.totalTokens()));
            }
            if (!fallback.toolCalls().isEmpty() && onToolCalls != null) {
                onToolCalls.accept(fallback.toolCalls());
            }
        }
    }

    /** 流式 tool_calls 碎片聚合器：按 index 归位、拼接 arguments 字符串，只回调一次 */
    private static final class ToolCallAggregator {
        private final TreeMap<Integer, String> ids = new TreeMap<>();
        private final TreeMap<Integer, String> names = new TreeMap<>();
        private final TreeMap<Integer, StringBuilder> arguments = new TreeMap<>();
        private boolean tokensEmitted;
        private boolean fired;

        void merge(JsonNode toolCallDeltas) {
            for (JsonNode node : toolCallDeltas) {
                int index = node.path("index").asInt(0);
                String id = node.path("id").asText("");
                String name = node.path("function").path("name").asText("");
                String fragment = node.path("function").path("arguments").asText("");
                if (!id.isEmpty()) {
                    ids.put(index, id);
                }
                if (!name.isEmpty()) {
                    names.put(index, name);
                }
                if (!fragment.isEmpty()) {
                    arguments.computeIfAbsent(index, k -> new StringBuilder()).append(fragment);
                }
            }
        }

        void fireOnce(Consumer<List<ToolCall>> onToolCalls) {
            if (fired || onToolCalls == null || names.isEmpty()) {
                return;
            }
            List<ToolCall> calls = new ArrayList<>();
            for (Map.Entry<Integer, String> entry : names.entrySet()) {
                int index = entry.getKey();
                StringBuilder args = arguments.get(index);
                String argumentsJson = args == null || args.isEmpty() ? "{}" : args.toString();
                calls.add(new ToolCall(ids.getOrDefault(index, ""), entry.getValue(), argumentsJson));
            }
            fired = true;
            onToolCalls.accept(calls);
        }
    }

    /** 流式用量快照 */
    public record StreamUsage(Integer promptTokens, Integer completionTokens, Integer totalTokens) {
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        // 识别任意版本号结尾（/v1、/v4…）均不再追加，避免智谱 paas/v4 被拼成 /v4/v1（2026-08-21 实测踩坑）
        return normalized.matches(".*/v\\d+") ? normalized : normalized + "/v1";
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
