package com.aurora.ai.audit.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aurora.common.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_tool_call_log")
public class AiToolCallLogDO extends BaseDO {
    private Long sessionId;
    private Long actionId;
    private Long userId;
    private Long providerId;
    private String toolName;
    private String permissionCode;
    private String paramsSummary;
    private Integer success;
    private String resultSummary;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}