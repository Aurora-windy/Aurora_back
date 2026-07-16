package com.aurora.ai.audit.model.resp;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AiToolCallLogResp {
    private Long id;
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
    private LocalDateTime createTime;
}
