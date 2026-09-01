package com.aurora.ai.mcp.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.ai.mcp.demo.McpDemoProcessManager;
import com.aurora.ai.mcp.service.AiMcpServerService;
import com.aurora.common.constant.PermCodeConst;
import com.aurora.common.response.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/admin/mcp-demo")
public class AiMcpDemoController {
    private final McpDemoProcessManager processManager;
    private final AiMcpServerService serverService;

    @SaCheckPermission(PermCodeConst.Ai.Mcp.SYNC)
    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        return Result.ok(toMap(processManager.status()));
    }

    @SaCheckPermission(PermCodeConst.Ai.Mcp.SYNC)
    @PostMapping("/start")
    public Result<Map<String, Object>> start() {
        processManager.start();
        serverService.syncAllEnabled();
        return Result.ok(toMap(processManager.status()));
    }

    @SaCheckPermission(PermCodeConst.Ai.Mcp.SYNC)
    @PostMapping("/stop")
    public Result<Map<String, Object>> stop() {
        serverService.disconnectAllEnabled();
        return Result.ok(toMap(processManager.stop()));
    }

    private Map<String, Object> toMap(McpDemoProcessManager.DemoStatus status) {
        return Map.of("running", status.running(), "port", status.port());
    }
}
