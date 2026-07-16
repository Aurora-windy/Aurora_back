package com.aurora.ai.agent.model.resp;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
public class ActionResp implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long sessionId;
    private Long messageId;
    private String actionType;
    private String toolName;
    private String planSummary;
    private String paramsJson;
    private String riskSummary;
    private String status;
    private String resultSummary;
    private String errorMessage;
    private LocalDateTime confirmedAt;
    private LocalDateTime executedAt;
}