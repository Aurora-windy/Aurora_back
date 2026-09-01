package com.aurora.ai.mcp.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.ai.mcp.model.req.McpServerEnabledReq;
import com.aurora.ai.mcp.model.req.McpServerPageReq;
import com.aurora.ai.mcp.model.req.McpServerSaveReq;
import com.aurora.ai.mcp.model.resp.McpServerResp;
import com.aurora.ai.mcp.model.resp.McpServerSyncResp;
import com.aurora.ai.mcp.service.AiMcpServerService;
import com.aurora.common.constant.PermCodeConst;
import com.aurora.common.response.PageResult;
import com.aurora.common.response.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * MCP Server 管理控制器（T-M2）。
 * <p>
 * 后台 CRUD 接口：分页查询、创建、更新、启用/禁用、删除、手动同步。
 * 权限码复用 AI Provider 的权限体系（PermCodeConst.Ai.Mcp.*）。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class AiMcpServerController {

    private final AiMcpServerService serverService;

    @SaCheckPermission(PermCodeConst.Ai.Mcp.LIST)
    @GetMapping("/admin/mcp-servers")
    public Result<PageResult<McpServerResp>> page(McpServerPageReq req) {
        return Result.ok(serverService.page(req));
    }

    @SaCheckPermission(PermCodeConst.Ai.Mcp.LIST)
    @GetMapping("/admin/mcp-servers/enabled")
    public Result<List<McpServerResp>> enabled() {
        return Result.ok(serverService.listEnabled());
    }

    @SaCheckPermission(PermCodeConst.Ai.Mcp.CREATE)
    @PostMapping("/admin/mcp-servers")
    public Result<Long> create(@RequestBody @Valid McpServerSaveReq req) {
        return Result.ok(serverService.create(req));
    }

    @SaCheckPermission(PermCodeConst.Ai.Mcp.UPDATE)
    @PutMapping("/admin/mcp-servers/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody @Valid McpServerSaveReq req) {
        serverService.update(id, req);
        return Result.ok(Boolean.TRUE);
    }

    @SaCheckPermission(PermCodeConst.Ai.Mcp.UPDATE)
    @PatchMapping("/admin/mcp-servers/{id}/enabled")
    public Result<Boolean> setEnabled(@PathVariable Long id, @RequestBody @Valid McpServerEnabledReq req) {
        serverService.setEnabled(id, req);
        return Result.ok(Boolean.TRUE);
    }

    @SaCheckPermission(PermCodeConst.Ai.Mcp.SYNC)
    @PostMapping("/admin/mcp-servers/{id}/sync")
    public Result<McpServerSyncResp> sync(@PathVariable Long id) {
        return Result.ok(serverService.sync(id));
    }

    @SaCheckPermission(PermCodeConst.Ai.Mcp.DELETE)
    @DeleteMapping("/admin/mcp-servers/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        serverService.delete(id);
        return Result.ok(Boolean.TRUE);
    }
}
