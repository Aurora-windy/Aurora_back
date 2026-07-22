package com.aurora.ai.builder.model.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuilderGeneratedFileResp {
    private String path;
    private String type;
    private String description;
    private Long sizeBytes;
}
