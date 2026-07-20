package com.aurora.ai.builder.model.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuilderPlanResp {
    private String requestId;
    private String requirement;
    private List<BuilderModuleResp> selectedModules;
    private List<String> excludedModules;
    private List<String> missingDependencies;
    private List<String> warnings;
    private String nextStep;
}