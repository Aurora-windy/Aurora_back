package com.aurora.ai.mcp.server;

import com.aurora.ai.tool.core.AiToolDefinition;
import com.aurora.ai.tool.core.AiToolRegistry;
import com.aurora.ai.tool.core.AiToolSchemaGenerator;
import com.aurora.ai.tool.model.AiToolRequest;
import com.aurora.ai.tool.model.AiToolResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AURORA MCP Server Controller（T-M3 修正版）。
 * <p>
 * MCP SDK 2.0.0 不提供 Spring MVC 传输层，此处用 SseEmitter 手动实现 MCP SSE 协议。
 * 仅当 aurora.ai.mcp.server.enabled=true 时激活。
 * 暴露 EDU 查询工具给外部 MCP client（如 Claude Desktop）。
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
@ConditionalOnProperty(name = "aurora.ai.mcp.server.enabled", havingValue = "true")
@RequiredArgsConstructor
public class AuroraMcpController {

    private final McpServerProperties properties;
    private final AiToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    /** 活跃的 SSE 连接（sessionId → emitter） */
    private final Map<String, SseEmitter> activeSessions = new ConcurrentHashMap<>();

    /** 已注册的工具列表（供 tools/list 返回） */
    private List<McpSchema.Tool> registeredTools;

    @PostConstruct
    public void init() {
        // 收集可暴露的工具（仅查询类，mutation 工具不暴露）
        List<AiToolDefinition> exposedTools = toolRegistry.list().stream()
                .filter(t -> !Boolean.TRUE.equals(t.getMutation()))
                // 本地工作区能力是会话级权限，不对无会话的外部 MCP client 暴露。
                .filter(t -> t.getRequiredCapability() == null)
                .toList();

        registeredTools = exposedTools.stream()
                .map(this::buildMcpTool)
                .toList();

        log.info("mcp.server started tools={} endpoint=/mcp", registeredTools.size());
    }

    /**
     * SSE 端点：客户端建立长连接，服务器通过此连接推送消息。
     * GET /mcp/sse?token=xxx
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sse(@RequestParam(value = "token", required = false) String token) {
        // Token 校验由 McpServerSecurityFilter 处理，这里只做基本检查
        SseEmitter emitter = new SseEmitter(300_000L); // 5分钟超时
        String sessionId = UUID.randomUUID().toString();
        activeSessions.put(sessionId, emitter);

        emitter.onCompletion(() -> activeSessions.remove(sessionId));
        emitter.onTimeout(() -> activeSessions.remove(sessionId));
        emitter.onError(e -> activeSessions.remove(sessionId));

        // 发送初始化事件（MCP 协议要求 server 先发送 capabilities）
        try {
            Map<String, Object> capabilities = Map.of(
                    "tools", Map.of(),
                    "resources", Map.of(),
                    "prompts", Map.of()
            );
            emitter.send(SseEmitter.event()
                    .name("endpoint")
                    // 使用相对路径，兼容应用部署在 /api 等 context-path 下。
                    .data("message?sessionId=" + sessionId));
        } catch (IOException e) {
            log.warn("mcp.sse init failed sessionId={}", sessionId, e);
        }

        log.info("mcp.sse connected sessionId={}", sessionId);
        return emitter;
    }

    /**
     * 消息端点：客户端发送 JSON-RPC 请求。
     * POST /mcp/message?token=xxx
     */
    @PostMapping(value = "/message", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> handleMessage(
            @RequestBody Map<String, Object> request,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestParam(value = "token", required = false) String token) {

        String method = (String) request.get("method");
        Object id = request.get("id");

        // JSON-RPC notifications (for example notifications/initialized) have no
        // id and must never receive a response with id=null.
        if (id == null && method != null && method.startsWith("notifications/")) {
            return ResponseEntity.accepted().build();
        }

        ResponseEntity<?> response;
        try {
            response = switch (method) {
                case "initialize" -> handleInitialize(id);
                case "tools/list" -> handleToolsList(id);
                case "tools/call" -> handleToolsCall(id, request);
                default -> jsonRpcError(id, -32601, "Method not found: " + method);
            };
        } catch (Exception ex) {
            log.error("mcp.message error method={} id={}", method, id, ex);
            response = jsonRpcError(id, -32603, "Internal error: " + ex.getMessage());
        }

        // MCP SSE clients consume JSON-RPC responses from the open SSE stream.
        // Keep the direct response fallback for manual HTTP diagnostics.
        if (StringUtils.hasText(sessionId)) {
            SseEmitter emitter = activeSessions.get(sessionId);
            if (emitter == null) {
                return jsonRpcError(id, -32000, "Unknown MCP session: " + sessionId);
            }
            try {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(objectMapper.writeValueAsString(response.getBody())));
                return ResponseEntity.accepted().build();
            } catch (IOException ex) {
                activeSessions.remove(sessionId);
                log.warn("mcp.message response failed sessionId={} method={}", sessionId, method, ex);
                return jsonRpcError(id, -32603, "Failed to send MCP response");
            }
        }
        return response;
    }

    /** initialize 握手 */
    private ResponseEntity<?> handleInitialize(Object id) {
        Map<String, Object> result = Map.of(
                "protocolVersion", "2024-11-05",
                "capabilities", Map.of(
                        "tools", Map.of("listChanged", false),
                        "resources", Map.of(),
                        "prompts", Map.of()
                ),
                "serverInfo", Map.of(
                        "name", properties.getServerName(),
                        "version", properties.getServerVersion()
                )
        );
        return jsonRpcResponse(id, result);
    }

    /** tools/list 返回可用工具列表 */
    private ResponseEntity<?> handleToolsList(Object id) {
        List<Map<String, Object>> tools = registeredTools.stream()
                .map(this::toolToMap)
                .toList();
        return jsonRpcResponse(id, Map.of("tools", tools));
    }

    /** tools/call 执行工具调用 */
    private ResponseEntity<?> handleToolsCall(Object id, Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) request.getOrDefault("params", Map.of());
        String toolName = (String) params.get("name");
        Object arguments = params.get("arguments");

        // 查找工具定义
        String displayName = AiToolSchemaGenerator.fromWireName(toolName);
        AiToolDefinition definition = findTool(displayName);
        if (definition == null) {
            return jsonRpcError(id, -32602, "Tool not found: " + displayName);
        }

        long started = System.currentTimeMillis();
        try {
            // 解析参数
            Map<String, Object> paramMap;
            if (arguments instanceof Map<?, ?> map) {
                paramMap = (Map<String, Object>) map;
            } else if (arguments instanceof String jsonStr) {
                paramMap = objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});
            } else {
                paramMap = Map.of();
            }

            // 直接调用 handler（绕过 AiToolExecutor 的 Sa-Token 权限检查）
            AiToolRequest aiRequest = AiToolRequest.builder()
                    .sessionId(null)
                    .userId(null)
                    .toolName(displayName)
                    .params(paramMap)
                    .build();
            AiToolResult result = definition.getHandler().execute(aiRequest);

            long duration = System.currentTimeMillis() - started;
            log.info("mcp.call success tool={} duration={}ms", displayName, duration);

            // 包装为 MCP CallToolResult
            String content;
            boolean isError = !Boolean.TRUE.equals(result.getSuccess());
            if (!isError) {
                content = objectMapper.writeValueAsString(result.getData());
            } else {
                content = "工具执行失败：" + result.getErrorMessage();
            }

            Map<String, Object> callResult = Map.of(
                    "content", List.of(Map.of("type", "text", "text", content)),
                    "isError", isError
            );
            return jsonRpcResponse(id, callResult);

        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - started;
            log.warn("mcp.call failed tool={} duration={}ms error={}", displayName, duration, ex.getMessage());
            return jsonRpcError(id, -32603, "Tool execution error: " + ex.getMessage());
        }
    }

    // ==================== JSON-RPC 辅助方法 ====================

    private ResponseEntity<?> jsonRpcResponse(Object id, Object result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("result", result);
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<?> jsonRpcError(Object id, int code, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("error", Map.of("code", code, "message", message));
        return ResponseEntity.ok(response);
    }

    // ==================== 工具构建辅助 ====================

    private McpSchema.Tool buildMcpTool(AiToolDefinition tool) {
        // 复用 AiToolSchemaGenerator 生成 FC 格式 schema，提取 parameters 作为 inputSchema
        Map<String, Object> fcSchema = new AiToolSchemaGenerator(objectMapper).generate(tool);
        @SuppressWarnings("unchecked")
        Map<String, Object> function = (Map<String, Object>) fcSchema.get("function");
        @SuppressWarnings("unchecked")
        Map<String, Object> inputSchema = function != null
                ? (Map<String, Object>) function.get("parameters")
                : Map.of("type", "object", "properties", Map.of());

        return McpSchema.Tool.builder(AiToolSchemaGenerator.toWireName(tool.getName()), inputSchema)
                .description(tool.getDescription())
                .build();
    }

    private Map<String, Object> toolToMap(McpSchema.Tool tool) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", tool.name());
        map.put("description", tool.description());
        map.put("inputSchema", tool.inputSchema());
        return map;
    }

    private AiToolDefinition findTool(String displayName) {
        try {
            AiToolDefinition definition = toolRegistry.get(displayName);
            // 外部 MCP 没有 AURORA 会话上下文，只能调用明确允许外部暴露的只读工具。
            if (Boolean.TRUE.equals(definition.getMutation()) || definition.getRequiredCapability() != null) {
                return null;
            }
            return definition;
        } catch (Exception ex) {
            return null;
        }
    }
}
