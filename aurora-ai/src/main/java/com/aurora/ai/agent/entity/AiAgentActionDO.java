package com.aurora.ai.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aurora.common.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_agent_action")
public class AiAgentActionDO extends BaseDO {
    private Long sessionId;
    private Long messageId;
    private Long userId;
    private String actionType;
    private String toolName;
    private String planSummary;
    private String paramsJson;
    private String riskSummary;
    private String status;
    private LocalDateTime confirmedAt;
    private LocalDateTime executedAt;
    private String resultSummary;
    private String errorMessage;
}