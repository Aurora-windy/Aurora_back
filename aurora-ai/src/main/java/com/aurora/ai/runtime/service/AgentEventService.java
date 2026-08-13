package com.aurora.ai.runtime.service;

import com.aurora.ai.runtime.model.resp.AgentRuntimeEventResp;

import java.util.List;

/** Agent 任务事件服务。 */
public interface AgentEventService {

    /** 持久化事件并生成同一任务内单调递增的 sequence。 */
    AgentRuntimeEventResp append(Long taskId, String eventType, Object payload);

    /** 查询指定序号之后的事件，供断连恢复时回放。 */
    List<AgentRuntimeEventResp> listAfter(Long taskId, Long afterSequence);
}
