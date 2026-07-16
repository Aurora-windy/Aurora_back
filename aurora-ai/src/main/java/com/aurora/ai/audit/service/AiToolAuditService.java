package com.aurora.ai.audit.service;

import com.aurora.ai.tool.core.AiToolDefinition;
import com.aurora.ai.tool.model.AiToolRequest;
import com.aurora.ai.tool.model.AiToolResult;

import java.time.LocalDateTime;

public interface AiToolAuditService {
    void record(AiToolRequest request, AiToolDefinition definition, AiToolResult result, LocalDateTime startedAt, LocalDateTime finishedAt);
}