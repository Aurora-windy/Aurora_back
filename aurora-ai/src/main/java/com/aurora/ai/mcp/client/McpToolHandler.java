package com.aurora.ai.mcp.client;

import com.aurora.ai.tool.core.AiToolHandler;
import com.aurora.ai.tool.model.AiToolRequest;
import com.aurora.ai.tool.model.AiToolResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * MCP 工具处理器（T-M2）。
 * <p>
 * 将 MCP 远程工具包装为 AiToolHandler，桥接 AiToolExecutor 的调用到 McpClientManager。
 * 所有 MCP 工具一律 mutation=true，走确认流（spec 决策 #3）。
 */
@Slf4j
@RequiredArgsConstructor
public class McpToolHandler implements AiToolHandler {

    private final McpClientManager clientManager;
    private final String serverCode;
    private final String remoteToolName;
    private final int timeoutSeconds;

    @Override
    public AiToolResult execute(AiToolRequest request) {
        long started = System.currentTimeMillis();
        try {
            // 从 request.params 提取参数（Map<String, Object>）
            Map<String, Object> arguments = request.getParams();
            if (arguments == null) {
                arguments = Map.of();
            }

            // 调用 MCP server
            Object result = clientManager.callTool(serverCode, remoteToolName, arguments, timeoutSeconds);

            long duration = System.currentTimeMillis() - started;
            log.info("mcp.handler success server={} tool={} duration={}ms", serverCode, remoteToolName, duration);

            // 包装结果为 AiToolResult
            // MCP 工具返回的是文本内容，放入 data 字段
            return AiToolResult.ok(result != null ? result.toString() : "", "MCP tool call completed");

        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - started;
            log.warn("mcp.handler failed server={} tool={} duration={}ms error={}", serverCode, remoteToolName, duration, ex.getMessage());

            // 降级处理：server 不可达时返回失败结果，不砸主流程
            return AiToolResult.fail("MCP_CALL_FAILED", "MCP server call failed: " + ex.getMessage());
        }
    }
}
