package com.aurora.ai.mcp.model.resp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * MCP Server 列表/详情响应（T-M2）。
 */
@Data
public class McpServerResp {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String baseUrl;
    /** 是否已配置 Bearer Token，不返回密文 */
    private Boolean hasBearerToken;
    private Integer timeoutSeconds;
    private Integer enabled;
    private Integer toolCount;
    private LocalDateTime lastSyncAt;
    private String lastError;
    private LocalDateTime createTime;
}
