package com.aurora.ai.mcp.model.resp;

import lombok.Data;

/**
 * MCP Server 同步结果响应（T-M2）。
 */
@Data
public class McpServerSyncResp {
    /** 同步是否成功 */
    private Boolean success;
    /** 同步到的工具数量 */
    private Integer toolCount;
    /** 错误信息（失败时） */
    private String errorMessage;
    /** 同步耗时（毫秒） */
    private Long durationMs;

    public static McpServerSyncResp success(int toolCount, long durationMs) {
        McpServerSyncResp resp = new McpServerSyncResp();
        resp.setSuccess(true);
        resp.setToolCount(toolCount);
        resp.setDurationMs(durationMs);
        return resp;
    }

    public static McpServerSyncResp fail(String errorMessage, long durationMs) {
        McpServerSyncResp resp = new McpServerSyncResp();
        resp.setSuccess(false);
        resp.setErrorMessage(errorMessage);
        resp.setDurationMs(durationMs);
        return resp;
    }
}
