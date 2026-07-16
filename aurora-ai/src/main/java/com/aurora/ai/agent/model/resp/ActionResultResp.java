package com.aurora.ai.agent.model.resp;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class ActionResultResp implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long actionId;
    private String status;
    private String resultSummary;
    private String errorMessage;
}