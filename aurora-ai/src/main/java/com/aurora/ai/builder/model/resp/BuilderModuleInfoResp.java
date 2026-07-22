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
public class BuilderModuleInfoResp {
    private String id;
    private String name;
    private String category;
    private String description;
    private List<String> dependencies;
    private List<String> features;
    private String status;
    private Boolean required;
}
