package com.aurora.ai.builder.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.ai.builder.model.resp.BuilderGeneratePreviewResp;
import com.aurora.ai.builder.model.resp.BuilderModuleInfoResp;
import com.aurora.ai.builder.model.resp.BuilderPlanResp;
import com.aurora.ai.builder.service.BuilderService;
import com.aurora.common.constant.PermCodeConst;
import com.aurora.common.response.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/builder")
public class BuilderController {

    private final BuilderService builderService;

    @SaCheckPermission(PermCodeConst.Builder.Module.LIST)
    @GetMapping("/modules")
    public Result<List<BuilderModuleInfoResp>> modules() {
        return Result.ok(builderService.listModules());
    }

    @SaCheckPermission(PermCodeConst.Builder.Parse.USE)
    @PostMapping("/parse")
    public Result<BuilderPlanResp> parse(@RequestBody Map<String, Object> req) {
        return Result.ok(builderService.parse(readString(req, "requirement")));
    }

    @SaCheckPermission(PermCodeConst.Builder.Generate.PREVIEW)
    @PostMapping("/generate-preview")
    public Result<BuilderGeneratePreviewResp> generatePreview(@RequestBody Map<String, Object> req) {
        return Result.ok(builderService.generatePreview(readString(req, "requirement"), readStringList(req, "moduleIds")));
    }

    private String readString(Map<String, Object> req, String key) {
        Object value = req.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private List<String> readStringList(Map<String, Object> req, String key) {
        Object value = req.get(key);
        if (!(value instanceof List<?> rawList)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : rawList) {
            if (item != null) {
                result.add(String.valueOf(item));
            }
        }
        return result;
    }
}
