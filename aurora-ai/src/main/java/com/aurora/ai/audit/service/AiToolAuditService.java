package com.aurora.ai.audit.service;

import com.aurora.ai.tool.core.AiToolDefinition;
import com.aurora.ai.tool.model.AiToolRequest;
import com.aurora.ai.tool.model.AiToolResult;
import com.aurora.ai.audit.model.req.AiToolCallLogPageReq;
import com.aurora.ai.audit.model.resp.AiToolCallLogResp;
import com.aurora.common.response.PageResult;

import java.time.LocalDateTime;

public interface AiToolAuditService {
    void record(AiToolRequest request, AiToolDefinition definition, AiToolResult result, LocalDateTime startedAt, LocalDateTime finishedAt);

    PageResult<AiToolCallLogResp> page(AiToolCallLogPageReq req);
}
