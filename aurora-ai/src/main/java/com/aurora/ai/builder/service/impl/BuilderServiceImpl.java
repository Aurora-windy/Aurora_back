package com.aurora.ai.builder.service.impl;

import com.aurora.ai.builder.model.req.BuilderParseReq;
import com.aurora.ai.builder.model.resp.BuilderModuleResp;
import com.aurora.ai.builder.model.resp.BuilderPlanResp;
import com.aurora.ai.builder.service.BuilderService;
import com.aurora.ai.builder.support.BuilderModuleRegistry;
import com.aurora.ai.builder.support.BuilderRequirementParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BuilderServiceImpl implements BuilderService {

    private final BuilderModuleRegistry moduleRegistry;
    private final BuilderRequirementParser requirementParser;

    @Override
    public List<BuilderModuleResp> listModules() {
        return moduleRegistry.listModules();
    }

    @Override
    public BuilderPlanResp parse(BuilderParseReq req) {
        return requirementParser.parse(req.getRequirement());
    }
}