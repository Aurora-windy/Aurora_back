package com.aurora.ai.mcp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aurora.common.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * MCP Server 配置实体（T-M2）。
 * 存储外部 MCP server 的连接信息，Client 方向（AURORA 连接外部 MCP server）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_mcp_server")
public class AiMcpServerDO extends BaseDO {
    /** 唯一标识，用于工具名前缀 mcp.<code>.<tool> */
    private String code;
    /** 显示名称 */
    private String name;
    /** 描述 */
    private String description;
    /** MCP server 的 HTTP SSE 端点 */
    private String baseUrl;
    /** 远程 MCP server 的 Bearer Token 密文 */
    private String bearerTokenCipher;
    /** 单次 tools/call 超时上限（秒） */
    private Integer timeoutSeconds;
    /** 启用状态：0=禁用 1=启用 */
    private Integer enabled;
    /** 最近一次同步的工具数量（同步后回填） */
    private Integer toolCount;
    /** 最近一次成功同步的时间 */
    private LocalDateTime lastSyncAt;
    /** 最近一次同步/调用的错误信息（降级记录） */
    private String lastError;
}
