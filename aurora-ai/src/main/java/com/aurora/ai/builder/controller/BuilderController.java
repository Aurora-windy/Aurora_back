package com.aurora.ai.builder.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.ai.builder.model.req.BuilderParseReq;
import com.aurora.ai.builder.model.resp.BuilderModuleResp;
import com.aurora.ai.builder.model.resp.BuilderPlanResp;
import com.aurora.ai.builder.service.BuilderService;
import com.aurora.common.constant.PermCodeConst;
import com.aurora.common.response.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/builder")
public class BuilderController {

    private final BuilderService builderService;

    @SaCheckPermission(PermCodeConst.Builder.Module.LIST)
    @GetMapping("/modules")
    public Result<List<BuilderModuleResp>> modules() {
        return Result.ok(builderService.listModules());
    }

    @SaCheckPermission(PermCodeConst.Builder.Parse.USE)
    @PostMapping("/parse")
    public Result<BuilderPlanResp> parse(@RequestBody @Valid BuilderParseReq req) {
        return Result.ok(builderService.parse(req));
    }
}