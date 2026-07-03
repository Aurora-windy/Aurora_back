package com.aurora.system.auth.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.aurora.common.response.Result;
import com.aurora.system.auth.model.req.LoginReq;
import com.aurora.system.auth.model.resp.CaptchaResp;
import com.aurora.system.auth.model.resp.LoginResp;
import com.aurora.system.auth.model.resp.UserInfoResp;
import com.aurora.system.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证 API
 */
@Tag(name = "认证 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @SaIgnore
    @Operation(summary = "获取图形验证码")
    @GetMapping("/captcha")
    public Result<CaptchaResp> captcha() {
        return Result.ok(authService.captcha());
    }

    @SaIgnore
    @Operation(summary = "账号登录")
    @PostMapping("/login")
    public Result<LoginResp> login(@RequestBody @Valid LoginReq req) {
        return Result.ok(authService.login(req));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public Result<?> logout() {
        authService.logout();
        return Result.ok();
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/user/info")
    public Result<UserInfoResp> userInfo() {
        return Result.ok(authService.userInfo());
    }
}
