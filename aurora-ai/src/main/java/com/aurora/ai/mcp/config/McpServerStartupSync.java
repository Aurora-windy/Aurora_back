package com.aurora.ai.mcp.config;

import com.aurora.ai.mcp.service.AiMcpServerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * MCP Server 启动同步器（T-M2）。
 * <p>
 * 应用启动完成后自动同步所有启用的 MCP server，将远程工具注册到 AiToolRegistry。
 * 单个 server 同步失败不影响其他 server 和主流程（降级策略）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpServerStartupSync {

    private final AiMcpServerService serverService;

    @Value("${aurora.ai.mcp.client.startup-sync-enabled:false}")
    private boolean startupSyncEnabled;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!startupSyncEnabled) {
            log.info("mcp.startup sync disabled by configuration");
            return;
        }
        log.info("mcp.startup sync begin");
        try {
            serverService.syncAllEnabled();
            log.info("mcp.startup sync completed");
        } catch (Exception ex) {
            log.error("mcp.startup sync failed", ex);
            // 启动同步失败不阻止应用启动
        }
    }
}
