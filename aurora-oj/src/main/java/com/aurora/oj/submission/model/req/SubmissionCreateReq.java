package com.aurora.oj.submission.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubmissionCreateReq {

    @NotNull
    private Long problemId;

    @NotBlank
    private String language;

    @NotBlank
    @Size(max = 20000)
    private String sourceCode;
}
