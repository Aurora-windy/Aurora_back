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
public class BuilderGeneratePreviewResp {
    private String buildId;
    private String requirement;
    private List<String> selectedModules;
    private List<BuilderGeneratedFileResp> generatedFiles;
    private List<String> warnings;
    private String nextStep;
}
