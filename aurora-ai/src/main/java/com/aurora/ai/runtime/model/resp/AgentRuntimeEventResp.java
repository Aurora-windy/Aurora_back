package com.aurora.ai.runtime.model.resp;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** Agent 运行时事件响应，供任务时间线和 SSE 回放使用。 */
@Data
@Builder
public class AgentRuntimeEventResp {
    private Long id;
    private Long taskId;
    private Long sequence;
    private String eventType;
    private String payloadJson;
    private LocalDateTime occurredAt;
}
