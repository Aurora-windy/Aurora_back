package com.aurora.system.user.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.common.constant.PermCodeConst;
import com.aurora.common.response.PageResult;
import com.aurora.common.response.Result;
import com.aurora.system.user.model.req.BatchIdsReq;
import com.aurora.system.user.model.req.UserAddReq;
import com.aurora.system.user.model.req.UserAssignRoleReq;
import com.aurora.system.user.model.req.UserPageReq;
import com.aurora.system.user.model.req.UserResetPasswordReq;
import com.aurora.system.user.model.req.UserStatusReq;
import com.aurora.system.user.model.req.UserUpdateReq;
import com.aurora.system.user.model.resp.UserResp;
import com.aurora.system.user.service.SysUserService;
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
 * 系统用户 API。
 */
@Tag(name = "系统管理 - 用户")
@RestController
@RequiredArgsConstructor
@RequestMapping("/system/users")
public class SysUserController {

    private final SysUserService userService;

    @Operation(summary = "用户分页")
    @SaCheckPermission(PermCodeConst.System.User.LIST)
    @GetMapping
    public Result<PageResult<UserResp>> page(UserPageReq req) {
        return Result.ok(userService.page(req));
    }

    @Operation(summary = "用户详情")
    @SaCheckPermission(PermCodeConst.System.User.LIST)
    @GetMapping("/{id}")
    public Result<UserResp> detail(@PathVariable Long id) {
        return Result.ok(userService.detail(id));
    }

    @Operation(summary = "新增用户")
    @SaCheckPermission(PermCodeConst.System.User.ADD)
    @PostMapping
    public Result<Long> add(@RequestBody @Valid UserAddReq req) {
        return Result.ok(userService.add(req));
    }

    @Operation(summary = "修改用户")
    @SaCheckPermission(PermCodeConst.System.User.EDIT)
    @PutMapping
    public Result<Boolean> update(@RequestBody @Valid UserUpdateReq req) {
        userService.update(req);
        return Result.ok(Boolean.TRUE);
    }

    @Operation(summary = "删除用户")
    @SaCheckPermission(PermCodeConst.System.User.REMOVE)
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok(Boolean.TRUE);
    }

    @Operation(summary = "批量删除用户")
    @SaCheckPermission(PermCodeConst.System.User.REMOVE)
    @DeleteMapping("/batch")
    public Result<Boolean> deleteBatch(@RequestBody @Valid BatchIdsReq req) {
        userService.deleteBatch(req);
        return Result.ok(Boolean.TRUE);
    }

    @Operation(summary = "用户启停")
    @SaCheckPermission(PermCodeConst.System.User.STATUS)
    @PostMapping("/status")
    public Result<Boolean> status(@RequestBody @Valid UserStatusReq req) {
        userService.updateStatus(req);
        return Result.ok(Boolean.TRUE);
    }

    @Operation(summary = "重置密码")
    @SaCheckPermission(PermCodeConst.System.User.RESET_PASSWORD)
    @PostMapping("/reset-password")
    public Result<Boolean> resetPassword(@RequestBody @Valid UserResetPasswordReq req) {
        userService.resetPassword(req);
        return Result.ok(Boolean.TRUE);
    }

    @Operation(summary = "分配角色")
    @SaCheckPermission(PermCodeConst.System.User.EDIT)
    @PostMapping("/assign-role")
    public Result<Boolean> assignRole(@RequestBody @Valid UserAssignRoleReq req) {
        userService.assignRole(req);
        return Result.ok(Boolean.TRUE);
    }
}
