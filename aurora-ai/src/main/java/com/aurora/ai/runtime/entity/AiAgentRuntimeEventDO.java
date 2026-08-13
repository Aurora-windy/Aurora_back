package com.aurora.ai.runtime.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aurora.common.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** Agent 运行时事件实体，sequence 保证单任务事件可回放。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_agent_runtime_event")
public class AiAgentRuntimeEventDO extends BaseDO {
    private Long taskId;
    private Long sequence;
    private String eventType;
    private String payloadJson;
    private LocalDateTime occurredAt;
}
