package com.aurora.ai.mcp.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * MCP Server 创建/更新请求（T-M2）。
 */
@Data
public class McpServerSaveReq {
    @NotBlank(message = "Server code is required")
    @Size(max = 64, message = "Code must be at most 64 characters")
    private String code;

    @NotBlank(message = "Server name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;

    @NotBlank(message = "Base URL is required")
    @Size(max = 500, message = "Base URL must be at most 500 characters")
    private String baseUrl;

    /** Bearer Token；编辑时留空表示沿用已保存的 Token */
    @Size(max = 1000, message = "Bearer token must be at most 1000 characters")
    private String bearerToken;

    /** 超时秒数，默认 10 */
    private Integer timeoutSeconds;
}
