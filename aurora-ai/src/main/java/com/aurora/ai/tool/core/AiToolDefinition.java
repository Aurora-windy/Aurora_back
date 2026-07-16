package com.aurora.ai.tool.core;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiToolDefinition {
    private String name;
    private String description;
    private String permissionCode;
    private Boolean mutation;
    private Class<?> parameterClass;
    private AiToolHandler handler;
}