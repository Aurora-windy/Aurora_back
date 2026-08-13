package com.aurora.ai.runtime.service.impl;

import com.aurora.ai.runtime.entity.AiAgentRuntimeEventDO;
import com.aurora.ai.runtime.mapper.AiAgentRuntimeEventMapper;
import com.aurora.ai.runtime.model.resp.AgentRuntimeEventResp;
import com.aurora.ai.runtime.service.AgentEventService;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/** 单体 Runtime 的任务事件实现，保证单 JVM 内每次追加的序号有序。 */
@Service
@RequiredArgsConstructor
public class AgentEventServiceImpl implements AgentEventService {

    private final AiAgentRuntimeEventMapper eventMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized AgentRuntimeEventResp append(Long taskId, String eventType, Object payload) {
        if (taskId == null || !StringUtils.hasText(eventType)) {
            throw new BizException(BizCode.PARAM_ERROR, "taskId and eventType are required");
        }
        Long maxSequence = eventMapper.selectMaxSequence(taskId);
        AiAgentRuntimeEventDO event = new AiAgentRuntimeEventDO();
        event.setTaskId(taskId);
        event.setSequence((maxSequence == null ? 0L : maxSequence) + 1);
        event.setEventType(eventType);
        event.setPayloadJson(toJson(payload));
        event.setOccurredAt(LocalDateTime.now());
        eventMapper.insert(event);
        return toResp(event);
    }

    @Override
    public List<AgentRuntimeEventResp> listAfter(Long taskId, Long afterSequence) {
        if (taskId == null) {
            throw new BizException(BizCode.PARAM_ERROR, "taskId is required");
        }
        long safeSequence = afterSequence == null || afterSequence < 0 ? 0L : afterSequence;
        return eventMapper.selectList(Wrappers.<AiAgentRuntimeEventDO>lambdaQuery()
                        .eq(AiAgentRuntimeEventDO::getTaskId, taskId)
                        .gt(AiAgentRuntimeEventDO::getSequence, safeSequence)
                        .orderByAsc(AiAgentRuntimeEventDO::getSequence))
                .stream()
                .map(this::toResp)
                .toList();
    }

    /** 事件负载必须序列化成功，避免存储不可回放的半成品事件。 */
    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload == null ? java.util.Map.of() : payload);
        } catch (JsonProcessingException ex) {
            throw new BizException(BizCode.PARAM_ERROR, "runtime event payload is not serializable");
        }
    }

    private AgentRuntimeEventResp toResp(AiAgentRuntimeEventDO event) {
        return AgentRuntimeEventResp.builder()
                .id(event.getId())
                .taskId(event.getTaskId())
                .sequence(event.getSequence())
                .eventType(event.getEventType())
                .payloadJson(event.getPayloadJson())
                .occurredAt(event.getOccurredAt())
                .build();
    }
}
