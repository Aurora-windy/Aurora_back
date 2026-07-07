package com.aurora.system.role.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.common.constant.PermCodeConst;
import com.aurora.common.response.PageResult;
import com.aurora.common.response.Result;
import com.aurora.system.role.model.req.RoleAddReq;
import com.aurora.system.role.model.req.RoleAssignMenuReq;
import com.aurora.system.role.model.req.RolePageReq;
import com.aurora.system.role.model.req.RoleStatusReq;
import com.aurora.system.role.model.req.RoleUpdateReq;
import com.aurora.system.role.model.resp.RoleResp;
import com.aurora.system.role.service.SysRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统角色 API。
 */
@Tag(name = "系统管理 - 角色")
@RestController
@RequiredArgsConstructor
@RequestMapping("/system/roles")
public class SysRoleController {

    private final SysRoleService roleService;

    @Operation(summary = "角色分页")
    @SaCheckPermission(PermCodeConst.System.Role.LIST)
    @GetMapping
    public Result<PageResult<RoleResp>> page(RolePageReq req) {
        return Result.ok(roleService.page(req));
    }

    @Operation(summary = "角色详情")
    @SaCheckPermission(PermCodeConst.System.Role.LIST)
    @GetMapping("/{id}")
    public Result<RoleResp> detail(@PathVariable Long id) {
        return Result.ok(roleService.detail(id));
    }

    @Operation(summary = "新增角色")
    @SaCheckPermission(PermCodeConst.System.Role.ADD)
    @PostMapping
    public Result<Long> add(@RequestBody @Valid RoleAddReq req) {
        return Result.ok(roleService.add(req));
    }

    @Operation(summary = "修改角色")
    @SaCheckPermission(PermCodeConst.System.Role.EDIT)
    @PutMapping
    public Result<Boolean> update(@RequestBody @Valid RoleUpdateReq req) {
        roleService.update(req);
        return Result.ok(Boolean.TRUE);
    }

    @Operation(summary = "删除角色")
    @SaCheckPermission(PermCodeConst.System.Role.REMOVE)
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok(Boolean.TRUE);
    }

    @Operation(summary = "角色启停")
    @SaCheckPermission(PermCodeConst.System.Role.EDIT)
    @PostMapping("/status")
    public Result<Boolean> status(@RequestBody @Valid RoleStatusReq req) {
        roleService.updateStatus(req);
        return Result.ok(Boolean.TRUE);
    }

    @Operation(summary = "分配菜单")
    @SaCheckPermission(PermCodeConst.System.Role.ASSIGN_MENU)
    @PostMapping("/assign-menu")
    public Result<Boolean> assignMenu(@RequestBody @Valid RoleAssignMenuReq req) {
        roleService.assignMenu(req);
        return Result.ok(Boolean.TRUE);
    }
}
