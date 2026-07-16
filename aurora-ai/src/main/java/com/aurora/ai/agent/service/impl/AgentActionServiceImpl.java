package com.aurora.ai.agent.service.impl;

import com.aurora.ai.agent.entity.AiAgentActionDO;
import com.aurora.ai.agent.mapper.AiAgentActionMapper;
import com.aurora.ai.agent.model.resp.ActionResultResp;
import com.aurora.ai.agent.service.AgentActionService;
import com.aurora.ai.agent.support.AgentActionStatus;
import com.aurora.ai.tool.core.AiToolExecutor;
import com.aurora.ai.tool.model.AiToolRequest;
import com.aurora.ai.tool.model.AiToolResult;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.aurora.common.util.SecurityUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AgentActionServiceImpl implements AgentActionService {

    private final AiAgentActionMapper actionMapper;
    private final AiToolExecutor toolExecutor;
    private final ObjectMapper objectMapper;

    @Override
    public ActionResultResp confirm(Long actionId) {
        AiAgentActionDO action = requireOwnPendingAction(actionId);
        action.setConfirmedAt(LocalDateTime.now());
        AiToolResult result = toolExecutor.execute(AiToolRequest.builder()
                .sessionId(action.getSessionId())
                .actionId(action.getId())
                .userId(action.getUserId())
                .toolName(action.getToolName())
                .params(parseParams(action.getParamsJson()))
                .build());
        action.setExecutedAt(LocalDateTime.now());
        action.setStatus(Boolean.TRUE.equals(result.getSuccess()) ? AgentActionStatus.EXECUTED : AgentActionStatus.FAILED);
        action.setResultSummary(result.getSummary());
        action.setErrorMessage(result.getErrorMessage());
        actionMapper.updateById(action);
        return toResult(action);
    }

    @Override
    public ActionResultResp reject(Long actionId) {
        AiAgentActionDO action = requireOwnPendingAction(actionId);
        action.setStatus(AgentActionStatus.REJECTED);
        action.setResultSummary("User rejected the pending action. No EDU data was changed.");
        actionMapper.updateById(action);
        return toResult(action);
    }

    private AiAgentActionDO requireOwnPendingAction(Long actionId) {
        Long userId = SecurityUtil.requireUserId();
        AiAgentActionDO action = actionMapper.selectById(actionId);
        if (action == null) {
            throw new BizException(BizCode.DATA_NOT_FOUND, "Agent action does not exist");
        }
        if (!userId.equals(action.getUserId())) {
            throw new BizException(BizCode.FORBIDDEN, "Agent action does not belong to current user");
        }
        if (!AgentActionStatus.PENDING_CONFIRM.equals(action.getStatus())) {
            throw new BizException(BizCode.OPERATION_FAIL, "Only pending actions can be confirmed or rejected");
        }
        return action;
    }

    private Map<String, Object> parseParams(String paramsJson) {
        try {
            if (paramsJson == null || paramsJson.isBlank()) {
                return Map.of();
            }
            return objectMapper.readValue(paramsJson, new TypeReference<>() {});
        } catch (Exception ex) {
            throw new BizException(BizCode.PARAM_ERROR, "Invalid action params");
        }
    }

    private ActionResultResp toResult(AiAgentActionDO action) {
        return ActionResultResp.builder()
                .actionId(action.getId())
                .status(action.getStatus())
                .resultSummary(action.getResultSummary())
                .errorMessage(action.getErrorMessage())
                .build();
    }
}