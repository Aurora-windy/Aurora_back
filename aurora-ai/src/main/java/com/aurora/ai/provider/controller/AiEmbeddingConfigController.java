package com.aurora.ai.provider.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.ai.provider.model.req.EmbeddingConfigSaveReq;
import com.aurora.ai.provider.model.resp.EmbeddingConfigResp;
import com.aurora.ai.provider.model.resp.ProviderTestResp;
import com.aurora.ai.provider.service.AiEmbeddingConfigService;
import com.aurora.common.constant.PermCodeConst;
import com.aurora.common.response.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/admin/embedding-config")
public class AiEmbeddingConfigController {

    private final AiEmbeddingConfigService embeddingConfigService;

    @SaCheckPermission(PermCodeConst.Ai.Provider.LIST)
    @GetMapping
    public Result<EmbeddingConfigResp> get() {
        return Result.ok(embeddingConfigService.get());
    }

    @SaCheckPermission(PermCodeConst.Ai.Provider.UPDATE)
    @PutMapping
    public Result<Long> save(@RequestBody @Valid EmbeddingConfigSaveReq req) {
        return Result.ok(embeddingConfigService.save(req));
    }

    @SaCheckPermission(PermCodeConst.Ai.Provider.TEST)
    @PostMapping("/test")
    public Result<ProviderTestResp> test(@RequestBody @Valid EmbeddingConfigSaveReq req) {
        return Result.ok(embeddingConfigService.test(req));
    }
}