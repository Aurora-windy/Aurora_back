package com.aurora.ai.builder.service;

import com.aurora.ai.builder.model.resp.BuilderGeneratePreviewResp;
import com.aurora.ai.builder.model.resp.BuilderModuleInfoResp;
import com.aurora.ai.builder.model.resp.BuilderPlanResp;

import java.util.List;

public interface BuilderService {
    List<BuilderModuleInfoResp> listModules();

    BuilderPlanResp parse(String requirement);

    BuilderGeneratePreviewResp generatePreview(String requirement, List<String> moduleIds);
}
