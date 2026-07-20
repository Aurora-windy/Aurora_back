package com.aurora.ai.tool.core;

import cn.dev33.satoken.stp.StpUtil;
import com.aurora.ai.audit.service.AiToolAuditService;
import com.aurora.ai.tool.model.AiToolRequest;
import com.aurora.ai.tool.model.AiToolResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AiToolExecutor {

    private final AiToolRegistry registry;
    private final AiToolAuditService auditService;

    public AiToolResult execute(AiToolRequest request) {
        AiToolDefinition definition = registry.get(request.getToolName());
        LocalDateTime startedAt = LocalDateTime.now();
        AiToolResult result;
        try {
            StpUtil.checkPermission(definition.getPermissionCode());
            result = definition.getHandler().execute(request);
        } catch (Exception ex) {
            result = AiToolResult.fail(ex.getClass().getSimpleName(), "tool execution failed");
        }
        auditService.record(request, definition, result, startedAt, LocalDateTime.now());
        return result;
    }
}