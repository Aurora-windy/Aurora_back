package com.aurora.ai.audit.service.impl;

import com.aurora.ai.audit.entity.AiToolCallLogDO;
import com.aurora.ai.audit.mapper.AiToolCallLogMapper;
import com.aurora.ai.audit.service.AiToolAuditService;
import com.aurora.ai.tool.core.AiToolDefinition;
import com.aurora.ai.tool.model.AiToolRequest;
import com.aurora.ai.tool.model.AiToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AiToolAuditServiceImpl implements AiToolAuditService {

    private final AiToolCallLogMapper logMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void record(AiToolRequest request, AiToolDefinition definition, AiToolResult result, LocalDateTime startedAt, LocalDateTime finishedAt) {
        AiToolCallLogDO log = new AiToolCallLogDO();
        log.setSessionId(request.getSessionId());
        log.setActionId(request.getActionId());
        log.setUserId(request.getUserId());
        log.setProviderId(request.getProviderId());
        log.setToolName(request.getToolName());
        log.setPermissionCode(definition.getPermissionCode());
        log.setParamsSummary(toSummary(request.getParams()));
        log.setSuccess(Boolean.TRUE.equals(result.getSuccess()) ? 1 : 0);
        log.setResultSummary(result.getSummary());
        log.setErrorCode(result.getErrorCode());
        log.setErrorMessage(result.getErrorMessage());
        log.setStartedAt(startedAt);
        log.setFinishedAt(finishedAt);
        logMapper.insert(log);
    }

    private String toSummary(Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            return json.length() > 2000 ? json.substring(0, 2000) : json;
        } catch (Exception ex) {
            return null;
        }
    }
}