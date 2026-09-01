package com.aurora.ai.mcp.model.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * MCP Server 启用/禁用请求（T-M2）。
 */
@Data
public class McpServerEnabledReq {
    @NotNull(message = "Enabled flag is required")
    private Integer enabled;
}
