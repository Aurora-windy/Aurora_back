package com.aurora.ai.provider.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.ai.provider.model.req.ProviderEnabledReq;
import com.aurora.ai.provider.model.req.ProviderPageReq;
import com.aurora.ai.provider.model.req.ProviderSaveReq;
import com.aurora.ai.provider.model.resp.ProviderOptionResp;
import com.aurora.ai.provider.model.resp.ProviderResp;
import com.aurora.ai.provider.model.resp.ProviderTestResp;
import com.aurora.ai.provider.service.AiProviderService;
import com.aurora.common.constant.PermCodeConst;
import com.aurora.common.response.PageResult;
import com.aurora.common.response.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class AiProviderController {

    private final AiProviderService providerService;

    @SaCheckPermission(PermCodeConst.Ai.Chat.USE)
    @GetMapping("/providers/enabled")
    public Result<List<ProviderOptionResp>> enabled() {
        return Result.ok(providerService.listEnabled());
    }

    @SaCheckPermission(PermCodeConst.Ai.Provider.LIST)
    @GetMapping("/admin/providers")
    public Result<PageResult<ProviderResp>> page(ProviderPageReq req) {
        return Result.ok(providerService.page(req));
    }

    @SaCheckPermission(PermCodeConst.Ai.Provider.CREATE)
    @PostMapping("/admin/providers")
    public Result<Long> create(@RequestBody @Valid ProviderSaveReq req) {
        return Result.ok(providerService.create(req));
    }

    @SaCheckPermission(PermCodeConst.Ai.Provider.UPDATE)
    @PutMapping("/admin/providers/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody @Valid ProviderSaveReq req) {
        providerService.update(id, req);
        return Result.ok(Boolean.TRUE);
    }

    @SaCheckPermission(PermCodeConst.Ai.Provider.UPDATE)
    @PatchMapping("/admin/providers/{id}/enabled")
    public Result<Boolean> setEnabled(@PathVariable Long id, @RequestBody @Valid ProviderEnabledReq req) {
        providerService.setEnabled(id, req);
        return Result.ok(Boolean.TRUE);
    }

    @SaCheckPermission(PermCodeConst.Ai.Provider.TEST)
    @PostMapping("/admin/providers/{id}/test")
    public Result<ProviderTestResp> test(@PathVariable Long id) {
        return Result.ok(providerService.test(id));
    }
}