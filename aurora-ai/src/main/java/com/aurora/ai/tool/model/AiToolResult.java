package com.aurora.ai.tool.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiToolResult {
    private Boolean success;
    private Object data;
    private String summary;
    private String errorCode;
    private String errorMessage;

    public static AiToolResult ok(Object data, String summary) {
        return AiToolResult.builder().success(Boolean.TRUE).data(data).summary(summary).build();
    }

    public static AiToolResult fail(String errorCode, String errorMessage) {
        return AiToolResult.builder().success(Boolean.FALSE).errorCode(errorCode).errorMessage(errorMessage).build();
    }
}