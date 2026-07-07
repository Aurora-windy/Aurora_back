package com.aurora.system.menu.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.common.constant.PermCodeConst;
import com.aurora.common.response.Result;
import com.aurora.system.menu.model.req.MenuAddReq;
import com.aurora.system.menu.model.req.MenuUpdateReq;
import com.aurora.system.menu.model.resp.MenuResp;
import com.aurora.system.menu.model.resp.MenuTreeResp;
import com.aurora.system.menu.model.resp.UserRouteResp;
import com.aurora.system.menu.service.SysMenuService;
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

import java.util.List;

/**
 * 系统菜单 API。
 */
@Tag(name = "系统管理 - 菜单")
@RestController
@RequiredArgsConstructor
@RequestMapping("/system/menus")
public class SysMenuController {

    private final SysMenuService menuService;

    @Operation(summary = "菜单树")
    @SaCheckPermission(PermCodeConst.System.Menu.LIST)
    @GetMapping("/tree")
    public Result<List<MenuTreeResp>> tree() {
        return Result.ok(menuService.tree());
    }

    @Operation(summary = "当前用户路由")
    @GetMapping("/user-routes")
    public Result<List<UserRouteResp>> userRoutes() {
        return Result.ok(menuService.userRoutes());
    }

    @Operation(summary = "菜单详情")
    @SaCheckPermission(PermCodeConst.System.Menu.LIST)
    @GetMapping("/{id}")
    public Result<MenuResp> detail(@PathVariable Long id) {
        return Result.ok(menuService.detail(id));
    }

    @Operation(summary = "新增菜单")
    @SaCheckPermission(PermCodeConst.System.Menu.ADD)
    @PostMapping
    public Result<Long> add(@RequestBody @Valid MenuAddReq req) {
        return Result.ok(menuService.add(req));
    }

    @Operation(summary = "修改菜单")
    @SaCheckPermission(PermCodeConst.System.Menu.EDIT)
    @PutMapping
    public Result<Boolean> update(@RequestBody @Valid MenuUpdateReq req) {
        menuService.update(req);
        return Result.ok(Boolean.TRUE);
    }

    @Operation(summary = "删除菜单")
    @SaCheckPermission(PermCodeConst.System.Menu.REMOVE)
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        menuService.delete(id);
        return Result.ok(Boolean.TRUE);
    }
}
