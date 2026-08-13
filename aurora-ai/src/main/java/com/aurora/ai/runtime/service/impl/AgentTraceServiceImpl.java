package com.aurora.ai.runtime.service.impl;

import com.aurora.ai.runtime.entity.AiAgentTraceDO;
import com.aurora.ai.runtime.mapper.AiAgentTraceMapper;
import com.aurora.ai.runtime.service.AgentTraceService;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/** Agent Trace 的持久化实现，保存调用树结构与受控摘要。 */
@Service
@RequiredArgsConstructor
public class AgentTraceServiceImpl implements AgentTraceService {

    private static final String RUNNING = "RUNNING";

    private final AiAgentTraceMapper traceMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long start(Long taskId, Long parentTraceId, String spanType, String spanName, String inputSummary) {
        if (taskId == null || !StringUtils.hasText(spanType) || !StringUtils.hasText(spanName)) {
            throw new BizException(BizCode.PARAM_ERROR, "taskId, spanType and spanName are required");
        }
        AiAgentTraceDO trace = new AiAgentTraceDO();
        trace.setTaskId(taskId);
        trace.setParentTraceId(parentTraceId);
        trace.setSpanType(spanType);
        trace.setSpanName(spanName);
        trace.setState(RUNNING);
        trace.setInputSummary(truncate(inputSummary));
        trace.setStartedAt(LocalDateTime.now());
        traceMapper.insert(trace);
        return trace.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finish(Long traceId, String state, String outputSummary, String errorCode) {
        if (traceId == null || !StringUtils.hasText(state)) {
            throw new BizException(BizCode.PARAM_ERROR, "traceId and state are required");
        }
        AiAgentTraceDO trace = traceMapper.selectById(traceId);
        if (trace == null) {
            throw new BizException(BizCode.DATA_NOT_FOUND, "Agent trace does not exist");
        }
        trace.setState(state);
        trace.setOutputSummary(truncate(outputSummary));
        trace.setErrorCode(truncate(errorCode, 64));
        trace.setFinishedAt(LocalDateTime.now());
        traceMapper.updateById(trace);
    }

    /** 对摘要长度做约束，避免执行结果无限制写入 Trace 表。 */
    private String truncate(String value) {
        return truncate(value, 2000);
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
