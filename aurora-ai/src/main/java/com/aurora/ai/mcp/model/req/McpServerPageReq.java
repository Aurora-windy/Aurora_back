package com.aurora.ai.mcp.model.req;

import com.aurora.common.base.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MCP Server 分页查询请求（T-M2）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class McpServerPageReq extends PageRequest {
    private String code;
    private String name;
    private Integer enabled;
}
