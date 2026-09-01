package com.aurora.ai.mcp.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * MCP Server Token 初始化器（T-M3）。
 * <p>
 * 启动时如果未配置 bearer-token，自动生成一个并打印到日志（仅展示一次）。
 * 用户需复制此 token 配置到 Claude Desktop 等 MCP client。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "aurora.ai.mcp.server.enabled", havingValue = "true")
@RequiredArgsConstructor
public class McpServerTokenInitializer {

    private final McpServerProperties properties;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String token = properties.resolveToken();

        // 如果用户没配置 token，警告 + 打印自动生成的
        if (properties.getBearerToken() == null || properties.getBearerToken().isBlank()) {
            log.warn("╔══════════════════════════════════════════════════════════════╗");
            log.warn("║  MCP Server 已启动，自动生成 Bearer Token（仅展示一次）      ║");
            log.warn("╠══════════════════════════════════════════════════════════════╣");
            log.warn("║  Token: {}  ║", padRight(token, 54));
            log.warn("║  SSE:   {}  ║", padRight(properties.getSseEndpoint(), 54));
            log.warn("║  Msg:   {}  ║", padRight(properties.getMessageEndpoint(), 54));
            log.warn("╚══════════════════════════════════════════════════════════════╝");
        } else {
            log.info("mcp.server started with configured token sse={} message={}",
                    properties.getSseEndpoint(), properties.getMessageEndpoint());
        }
    }

    private String padRight(String s, int length) {
        if (s == null) s = "";
        if (s.length() >= length) return s.substring(0, length);
        return s + " ".repeat(length - s.length());
    }
}
