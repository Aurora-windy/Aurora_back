package com.aurora.ai.runtime.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aurora.common.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** Agent 执行 Trace，统一关联运行时、节点、模型和工具调用。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_agent_trace")
public class AiAgentTraceDO extends BaseDO {
    private Long taskId;
    private Long parentTraceId;
    private String spanType;
    private String spanName;
    private String state;
    private String inputSummary;
    private String outputSummary;
    private String errorCode;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
