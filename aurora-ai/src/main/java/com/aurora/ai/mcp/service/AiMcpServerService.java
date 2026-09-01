package com.aurora.ai.mcp.service;

import com.aurora.ai.mcp.model.req.McpServerEnabledReq;
import com.aurora.ai.mcp.model.req.McpServerPageReq;
import com.aurora.ai.mcp.model.req.McpServerSaveReq;
import com.aurora.ai.mcp.model.resp.McpServerResp;
import com.aurora.ai.mcp.model.resp.McpServerSyncResp;
import com.aurora.common.response.PageResult;

import java.util.List;

/**
 * MCP Server 配置服务接口（T-M2）。
 */
public interface AiMcpServerService {

    /**
     * 分页查询 MCP Server 列表。
     */
    PageResult<McpServerResp> page(McpServerPageReq req);

    /**
     * 创建 MCP Server 配置。
     */
    Long create(McpServerSaveReq req);

    /**
     * 更新 MCP Server 配置。
     */
    void update(Long id, McpServerSaveReq req);

    /**
     * 设置启用/禁用状态。
     */
    void setEnabled(Long id, McpServerEnabledReq req);

    /**
     * 删除 MCP Server 配置。
     */
    void delete(Long id);

    /**
     * 手动触发同步指定 server 的工具列表。
     */
    McpServerSyncResp sync(Long id);

    /**
     * 同步所有启用的 server（启动时调用）。
     */
    void syncAllEnabled();

    /** Disconnect all enabled MCP servers and remove their dynamic tools. */
    void disconnectAllEnabled();

    /**
     * 获取所有启用的 server 列表（内部用）。
     */
    List<McpServerResp> listEnabled();
}
