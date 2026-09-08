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
    /** 调用入口，用于统一权限/审计链路区分。 */
    private String source;
}
