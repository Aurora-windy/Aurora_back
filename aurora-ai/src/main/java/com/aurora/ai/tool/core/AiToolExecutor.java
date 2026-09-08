package com.aurora.ai.tool.core;

import cn.dev33.satoken.stp.StpUtil;
import com.aurora.ai.audit.service.AiToolAuditService;
import com.aurora.ai.tool.model.AiToolRequest;
import com.aurora.ai.tool.model.AiToolResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiToolExecutor {

    private final AiToolRegistry registry;
    private final AiToolAuditService auditService;

    public AiToolResult execute(AiToolRequest request) {
        log.debug("ai.tool.execute source={} tool={} sessionId={}",
                request.getSource() == null ? "UNKNOWN" : request.getSource(),
                request.getToolName(), request.getSessionId());
        AiToolDefinition definition = registry.get(request.getToolName());
        LocalDateTime startedAt = LocalDateTime.now();
        AiToolResult result;
        try {
            // Public/read-only tools may intentionally omit a permission code.
            // MCP has its own Bearer-token boundary and no AURORA login context. The
            // server controller has already applied the external read-only whitelist;
            // mutation/session-capability tools never reach this branch.
            boolean mcpReadOnly = "MCP_SERVER".equals(request.getSource())
                    && !Boolean.TRUE.equals(definition.getMutation())
                    && definition.getRequiredCapability() == null;
            if (!mcpReadOnly && StringUtils.hasText(definition.getPermissionCode())) {
                StpUtil.checkPermission(definition.getPermissionCode());
            }
            result = definition.getHandler().execute(request);
        } catch (Exception ex) {
            result = AiToolResult.fail(ex.getClass().getSimpleName(), "tool execution failed");
        }
        auditService.record(request, definition, result, startedAt, LocalDateTime.now());
        return result;
    }
}
