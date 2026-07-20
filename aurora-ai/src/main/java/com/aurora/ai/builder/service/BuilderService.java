package com.aurora.ai.builder.service;

import com.aurora.ai.builder.model.req.BuilderParseReq;
import com.aurora.ai.builder.model.resp.BuilderModuleResp;
import com.aurora.ai.builder.model.resp.BuilderPlanResp;

import java.util.List;

public interface BuilderService {
    List<BuilderModuleResp> listModules();

    BuilderPlanResp parse(BuilderParseReq req);
}