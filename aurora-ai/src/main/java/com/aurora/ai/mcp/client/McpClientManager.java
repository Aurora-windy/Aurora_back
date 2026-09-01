package com.aurora.ai.mcp.client;

import com.aurora.ai.mcp.entity.AiMcpServerDO;
import com.aurora.ai.provider.support.AiSecretCipher;
import com.aurora.ai.tool.core.AiToolDefinition;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP Client 管理器（T-M2 核心）。
 * <p>
 * 职责：
 * 1. 维护与外部 MCP server 的连接（initialize + tools/list）
 * 2. 将远程工具同步为 AiToolDefinition 注册到 AiToolRegistry
 * 3. 桥接 tools/call 调用到 MCP server
 * 4. 超时降级：server 不可达时剔除其工具，chat 主链路不报错
 * <p>
 * 设计决策：
 * - 每个 server 一个 McpSyncClient 实例，由 ConcurrentHashMap 管理生命周期
 * - 工具名格式：mcp.<serverCode>.<toolName>（点号经 toWireName 映射为 mcp__<serverCode>__<toolName>）
 * - 所有 MCP 工具一律 mutation=true（spec 决策 #3：外部 server 不可信，统一走确认流）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpClientManager {

    /** server code → McpSyncClient 实例 */
    private final Map<String, McpSyncClient> clients = new ConcurrentHashMap<>();
    /** server code → 该 server 提供的工具名列表（用于注销时批量移除） */
    private final Map<String, List<String>> serverTools = new ConcurrentHashMap<>();

    /**
     * 连接指定 server 并同步工具列表。
     *
     * @return 同步到的工具定义列表（已设置好 name/description/toolsSchemaJson/handler）
     */
    public List<AiToolDefinition> connectAndSync(AiMcpServerDO server) {
        String code = server.getCode();
        long started = System.currentTimeMillis();
        try {
            // 关闭旧连接（如果存在）
            disconnect(code);

            // 创建 transport + client
            URI sseUri = URI.create(server.getBaseUrl());
            String rawPath = sseUri.getPath();
            int lastSlash = rawPath == null ? -1 : rawPath.lastIndexOf('/');
            String basePath = lastSlash >= 0 ? rawPath.substring(0, lastSlash + 1) : "/";
            if (!basePath.endsWith("/")) {
                basePath += "/";
            }
            URI baseUri = new URI(sseUri.getScheme(), sseUri.getAuthority(), basePath, null, null);
            String sseEndpoint = rawPath == null || lastSlash < 0
                    ? "sse"
                    : rawPath.substring(lastSlash + 1);
            if (!StringUtils.hasText(sseEndpoint)) {
                sseEndpoint = "sse";
            }
            HttpClientSseClientTransport.Builder transportBuilder =
                    HttpClientSseClientTransport.builder(baseUri.toString()).sseEndpoint(sseEndpoint);
            if (StringUtils.hasText(server.getBearerTokenCipher())) {
                String bearerToken = AiSecretCipher.decrypt(server.getBearerTokenCipher());
                transportBuilder.httpRequestCustomizer((requestBuilder, method, uri, body, context) ->
                        requestBuilder.header("Authorization", "Bearer " + bearerToken));
            }
            HttpClientSseClientTransport transport = transportBuilder.build();
            McpSyncClient client = McpClient.sync(transport)
                    .requestTimeout(Duration.ofSeconds(server.getTimeoutSeconds() > 0 ? server.getTimeoutSeconds() : 10))
                    .clientInfo(new McpSchema.Implementation("AURORA", "1.0.0"))
                    .build();

            // initialize 握手
            client.initialize();
            clients.put(code, client);

            // tools/list 获取远程工具
            McpSchema.ListToolsResult toolsResult = client.listTools();
            List<McpSchema.Tool> tools = toolsResult.tools();
            List<AiToolDefinition> definitions = new ArrayList<>();
            List<String> toolNames = new ArrayList<>();

            for (McpSchema.Tool tool : tools) {
                String toolName = "mcp." + code + "." + tool.name();
                toolNames.add(toolName);

                // 将 MCP 的 inputSchema（JSON Schema 对象）序列化为字符串缓存
                String schemaJson = serializeInputSchema(tool.inputSchema());

                AiToolDefinition def = AiToolDefinition.builder()
                        .name(toolName)
                        .description(tool.description() != null ? tool.description() : "MCP tool: " + tool.name())
                        .mutation(true) // spec 决策 #3：MCP 工具一律走确认流
                        .toolsSchemaJson(schemaJson)
                        .handler(new McpToolHandler(this, code, tool.name(), server.getTimeoutSeconds()))
                        .build();
                definitions.add(def);
            }

            serverTools.put(code, toolNames);
            log.info("mcp.sync success server={} tools={} duration={}ms", code, toolNames.size(), System.currentTimeMillis() - started);
            return definitions;

        } catch (Exception ex) {
            log.warn("mcp.sync failed server={} duration={}ms error={}", code, System.currentTimeMillis() - started, ex.getMessage());
            // 同步失败：确保旧连接已清理
            disconnect(code);
            throw new IllegalStateException("MCP server connection failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 调用远程工具（由 McpToolHandler 回调）。
     */
    public Object callTool(String serverCode, String toolName, Map<String, Object> arguments, int timeoutSeconds) {
        McpSyncClient client = clients.get(serverCode);
        if (client == null) {
            throw new IllegalStateException("MCP server not connected: " + serverCode);
        }

        long started = System.currentTimeMillis();
        try {
            // 调用 tools/call（timeout 已在 client 构建时通过 requestTimeout 设置）
            McpSchema.CallToolResult result = client.callTool(
                    new McpSchema.CallToolRequest(toolName, arguments)
            );

            log.info("mcp.call success server={} tool={} duration={}ms", serverCode, toolName, System.currentTimeMillis() - started);

            // 返回结果内容（MCP 返回的是 Content 列表，取第一个 text）
            if (result.content() != null && !result.content().isEmpty()) {
                McpSchema.Content firstContent = result.content().get(0);
                if (firstContent instanceof McpSchema.TextContent textContent) {
                    return textContent.text();
                }
            }
            return result.isError() ? "Tool execution failed (MCP server returned error)" : "Tool executed successfully (no content)";

        } catch (Exception ex) {
            log.warn("mcp.call failed server={} tool={} duration={}ms error={}", serverCode, toolName, System.currentTimeMillis() - started, ex.getMessage());
            throw ex;
        }
    }

    /**
     * 断开指定 server 的连接。
     */
    public void disconnect(String serverCode) {
        McpSyncClient client = clients.remove(serverCode);
        if (client != null) {
            try {
                client.closeGracefully();
            } catch (Exception ex) {
                log.warn("mcp.disconnect error server={}", serverCode, ex);
            }
        }
        serverTools.remove(serverCode);
    }

    /**
     * 获取指定 server 提供的工具名列表。
     */
    public List<String> getToolNames(String serverCode) {
        return serverTools.getOrDefault(serverCode, List.of());
    }

    /**
     * 检查 server 是否已连接。
     */
    public boolean isConnected(String serverCode) {
        return clients.containsKey(serverCode);
    }

    /**
     * 将 MCP 的 inputSchema（JSON Schema 对象）序列化为 JSON 字符串。
     * MCP SDK 返回的是 Map<String, Object> 结构，直接序列化即可。
     */
    private String serializeInputSchema(Object inputSchema) {
        if (inputSchema == null) {
            return "{\"type\":\"object\",\"properties\":{}}";
        }
        try {
            // inputSchema 本身就是可序列化的 Map 结构
            if (inputSchema instanceof Map<?, ?> map) {
                // 使用 Jackson 序列化
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.writeValueAsString(map);
            }
            return inputSchema.toString();
        } catch (Exception ex) {
            log.warn("mcp.schema serialize failed, fallback to empty: {}", ex.getMessage());
            return "{\"type\":\"object\",\"properties\":{}}";
        }
    }
}
