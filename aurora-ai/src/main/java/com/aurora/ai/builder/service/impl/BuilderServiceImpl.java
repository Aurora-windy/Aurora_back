package com.aurora.ai.builder.service.impl;

import com.aurora.ai.builder.model.resp.BuilderGeneratePreviewResp;
import com.aurora.ai.builder.model.resp.BuilderModuleInfoResp;
import com.aurora.ai.builder.model.resp.BuilderPlanResp;
import com.aurora.ai.builder.service.BuilderService;
import com.aurora.ai.builder.support.BuilderModuleRegistry;
import com.aurora.ai.builder.support.BuilderPreviewGenerator;
import com.aurora.ai.builder.support.BuilderRequirementParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BuilderServiceImpl implements BuilderService {

    private final BuilderModuleRegistry moduleRegistry;
    private final BuilderRequirementParser requirementParser;
    private final BuilderPreviewGenerator previewGenerator;

    @Override
    public List<BuilderModuleInfoResp> listModules() {
        return moduleRegistry.listModules();
    }

    @Override
    public BuilderPlanResp parse(String requirement) {
        return requirementParser.parse(requirement);
    }

    @Override
    public BuilderGeneratePreviewResp generatePreview(String requirement, List<String> moduleIds) {
        return previewGenerator.generate(requirement, moduleIds);
    }
}
