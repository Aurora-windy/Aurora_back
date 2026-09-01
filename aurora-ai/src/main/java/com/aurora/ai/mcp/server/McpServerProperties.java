package com.aurora.ai.mcp.server;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * MCP Server 暴露配置（T-M3）。
 * <p>
 * 默认关闭（零暴露面）；开启时自动生成静态 Bearer token。
 * 仅暴露查询类工具（mutation 工具需走确认流，不适合外部 MCP client）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "aurora.ai.mcp.server")
public class McpServerProperties {

    /** MCP Server 总开关；false = endpoint 不注册（404） */
    private boolean enabled = false;

    /** SSE 端点路径（客户端建立 SSE 连接用） */
    private String sseEndpoint = "/mcp/sse";

    /** 消息端点路径（客户端发送 JSON-RPC 请求用） */
    private String messageEndpoint = "/mcp/message";

    /**
     * 静态 Bearer token。
     * 未配置时启动自动生成并打印到日志（仅展示一次）。
     */
    private String bearerToken;

    /** Server 显示名称 */
    private String serverName = "AURORA";

    /** Server 版本 */
    private String serverVersion = "1.0.0";

    /** 缓存自动生成的 token（避免每次调用 resolveToken 都生成新的） */
    private transient String resolvedToken;

    /**
     * 获取有效 token：优先用配置值，否则生成随机 UUID（仅生成一次并缓存）。
     */
    public String resolveToken() {
        if (bearerToken != null && !bearerToken.isBlank()) {
            return bearerToken;
        }
        if (resolvedToken == null) {
            resolvedToken = UUID.randomUUID().toString().replace("-", "");
        }
        return resolvedToken;
    }
}
