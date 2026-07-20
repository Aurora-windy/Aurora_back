package com.aurora.ai.builder.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BuilderParseReq {
    @NotBlank(message = "需求描述不能为空")
    @Size(max = 2000, message = "需求描述不能超过 2000 个字符")
    private String requirement;
}