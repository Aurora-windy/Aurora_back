package com.aurora.ai.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aurora.ai.audit.entity.AiToolCallLogDO;
import com.aurora.ai.audit.mapper.AiToolCallLogMapper;
import com.aurora.ai.audit.model.req.AiToolCallLogPageReq;
import com.aurora.ai.audit.model.resp.AiToolCallLogResp;
import com.aurora.ai.audit.service.AiToolAuditService;
import com.aurora.ai.tool.core.AiToolDefinition;
import com.aurora.ai.tool.model.AiToolRequest;
import com.aurora.ai.tool.model.AiToolResult;
import com.aurora.common.response.PageResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

    @Override
    public PageResult<AiToolCallLogResp> page(AiToolCallLogPageReq req) {
        LambdaQueryWrapper<AiToolCallLogDO> wrapper = Wrappers.<AiToolCallLogDO>lambdaQuery()
                .eq(req.getSessionId() != null, AiToolCallLogDO::getSessionId, req.getSessionId())
                .eq(req.getActionId() != null, AiToolCallLogDO::getActionId, req.getActionId())
                .eq(req.getUserId() != null, AiToolCallLogDO::getUserId, req.getUserId())
                .eq(req.getProviderId() != null, AiToolCallLogDO::getProviderId, req.getProviderId())
                .like(StringUtils.hasText(req.getToolName()), AiToolCallLogDO::getToolName, req.getToolName())
                .like(StringUtils.hasText(req.getPermissionCode()), AiToolCallLogDO::getPermissionCode, req.getPermissionCode())
                .eq(req.getSuccess() != null, AiToolCallLogDO::getSuccess, req.getSuccess())
                .orderByDesc(AiToolCallLogDO::getStartedAt)
                .orderByDesc(AiToolCallLogDO::getCreateTime);
        Page<AiToolCallLogDO> page = logMapper.selectPage(new Page<>(req.normalizedPageNum(), req.normalizedPageSize()), wrapper);
        return new PageResult<>(page.getRecords().stream().map(this::toResp).toList(), page.getTotal());
    }

    private String toSummary(Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            return json.length() > 2000 ? json.substring(0, 2000) : json;
        } catch (Exception ex) {
            return null;
        }
    }

    private AiToolCallLogResp toResp(AiToolCallLogDO log) {
        return AiToolCallLogResp.builder()
                .id(log.getId())
                .sessionId(log.getSessionId())
                .actionId(log.getActionId())
                .userId(log.getUserId())
                .providerId(log.getProviderId())
                .toolName(log.getToolName())
                .permissionCode(log.getPermissionCode())
                .paramsSummary(log.getParamsSummary())
                .success(log.getSuccess())
                .resultSummary(log.getResultSummary())
                .errorCode(log.getErrorCode())
                .errorMessage(log.getErrorMessage())
                .startedAt(log.getStartedAt())
                .finishedAt(log.getFinishedAt())
                .createTime(log.getCreateTime())
                .build();
    }
}
