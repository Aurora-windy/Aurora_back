package com.aurora.ai.tool.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class AiToolRequest {
    private Long sessionId;
    private Long actionId;
    private Long userId;
    private Long providerId;
    private String toolName;
    private Map<String, Object> params;
}