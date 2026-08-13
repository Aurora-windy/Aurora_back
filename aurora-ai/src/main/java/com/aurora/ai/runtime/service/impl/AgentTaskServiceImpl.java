package com.aurora.ai.runtime.service.impl;

import com.aurora.ai.runtime.entity.AiAgentTaskDO;
import com.aurora.ai.runtime.mapper.AiAgentTaskMapper;
import com.aurora.ai.runtime.model.req.CreateAgentTaskReq;
import com.aurora.ai.runtime.model.resp.AgentTaskResp;
import com.aurora.ai.runtime.service.AgentTaskService;
import com.aurora.ai.runtime.support.AgentTaskStatus;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.aurora.common.util.SecurityUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/** Agent 任务状态机的持久化实现。 */
@Service
@RequiredArgsConstructor
public class AgentTaskServiceImpl implements AgentTaskService {

    private static final int DEFAULT_MAX_RETRIES = 1;
    private static final int MAX_ALLOWED_RETRIES = 10;

    private final AiAgentTaskMapper taskMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentTaskResp create(CreateAgentTaskReq req) {
        Long userId = SecurityUtil.requireUserId();
        if (StringUtils.hasText(req.getIdempotencyKey())) {
            AiAgentTaskDO existing = taskMapper.selectOne(Wrappers.<AiAgentTaskDO>lambdaQuery()
                    .eq(AiAgentTaskDO::getUserId, userId)
                    .eq(AiAgentTaskDO::getIdempotencyKey, req.getIdempotencyKey()));
            if (existing != null) {
                return toResp(existing);
            }
        }

        int maxRetries = normalizeMaxRetries(req.getMaxRetries());
        AiAgentTaskDO task = new AiAgentTaskDO();
        task.setSessionId(req.getSessionId());
        task.setUserId(userId);
        task.setTaskType(req.getTaskType());
        task.setInputJson(req.getInputJson());
        task.setState(AgentTaskStatus.PENDING);
        task.setIdempotencyKey(StringUtils.hasText(req.getIdempotencyKey()) ? req.getIdempotencyKey() : null);
        task.setRetryCount(0);
        task.setMaxRetries(maxRetries);
        taskMapper.insert(task);
        return toResp(task);
    }

    @Override
    public AgentTaskResp getOwned(Long taskId) {
        return toResp(requireOwned(taskId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentTaskResp transition(Long taskId, String targetState, String reason) {
        AiAgentTaskDO task = requireOwned(taskId);
        AgentTaskStatus.requireTransition(task.getState(), targetState);
        LocalDateTime now = LocalDateTime.now();
        task.setState(targetState);

        // 首次进入运行态记录开始时间；终态统一记录结束时间。
        if (AgentTaskStatus.RUNNING.equals(targetState) && task.getStartedAt() == null) {
            task.setStartedAt(now);
        }
        if (AgentTaskStatus.isTerminal(targetState)) {
            task.setFinishedAt(now);
        }
        if (AgentTaskStatus.CANCELLED.equals(targetState)) {
            task.setCancelledAt(now);
        }
        if (AgentTaskStatus.FAILED.equals(targetState)) {
            task.setFailureCode("RUNTIME_FAILED");
            task.setFailureMessage("runtime execution failed");
        }
        taskMapper.updateById(task);
        return toResp(task);
    }

    @Override
    public AgentTaskResp cancel(Long taskId, String reason) {
        return transition(taskId, AgentTaskStatus.CANCELLED, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentTaskResp retry(Long taskId) {
        AiAgentTaskDO task = requireOwned(taskId);
        if (!AgentTaskStatus.FAILED.equals(task.getState())) {
            throw new BizException(BizCode.OPERATION_FAIL, "Only failed Agent tasks can be retried");
        }
        int retryCount = task.getRetryCount() == null ? 0 : task.getRetryCount();
        int maxRetries = task.getMaxRetries() == null ? DEFAULT_MAX_RETRIES : task.getMaxRetries();
        if (retryCount >= maxRetries) {
            throw new BizException(BizCode.OPERATION_FAIL, "Agent task retry limit exceeded");
        }
        task.setState(AgentTaskStatus.PENDING);
        task.setRetryCount(retryCount + 1);
        task.setStartedAt(null);
        task.setFinishedAt(null);
        task.setCancelledAt(null);
        task.setFailureCode(null);
        task.setFailureMessage(null);
        taskMapper.updateById(task);
        return toResp(task);
    }

    /** 查询并校验任务属于当前登录用户，防止跨用户读取或操作。 */
    private AiAgentTaskDO requireOwned(Long taskId) {
        Long userId = SecurityUtil.requireUserId();
        AiAgentTaskDO task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException(BizCode.DATA_NOT_FOUND, "Agent task does not exist");
        }
        if (!userId.equals(task.getUserId())) {
            throw new BizException(BizCode.FORBIDDEN, "Agent task does not belong to current user");
        }
        return task;
    }

    /** 约束重试上限，避免异常请求制造无限重试任务。 */
    private int normalizeMaxRetries(Integer maxRetries) {
        if (maxRetries == null) {
            return DEFAULT_MAX_RETRIES;
        }
        if (maxRetries < 0 || maxRetries > MAX_ALLOWED_RETRIES) {
            throw new BizException(BizCode.PARAM_ERROR, "maxRetries must be between 0 and 10");
        }
        return maxRetries;
    }

    private AgentTaskResp toResp(AiAgentTaskDO task) {
        return AgentTaskResp.builder()
                .id(task.getId())
                .sessionId(task.getSessionId())
                .userId(task.getUserId())
                .taskType(task.getTaskType())
                .inputJson(task.getInputJson())
                .state(task.getState())
                .idempotencyKey(task.getIdempotencyKey())
                .retryCount(task.getRetryCount())
                .maxRetries(task.getMaxRetries())
                .startedAt(task.getStartedAt())
                .finishedAt(task.getFinishedAt())
                .cancelledAt(task.getCancelledAt())
                .failureCode(task.getFailureCode())
                .failureMessage(task.getFailureMessage())
                .createTime(task.getCreateTime())
                .build();
    }
}
