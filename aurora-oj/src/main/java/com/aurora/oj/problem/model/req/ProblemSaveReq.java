package com.aurora.oj.problem.model.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProblemSaveReq {

    @NotBlank
    @Size(max = 120)
    private String title;

    @NotBlank
    private String description;

    @NotNull
    @Min(1)
    @Max(3)
    private Integer difficulty;

    @NotNull
    @Min(100)
    private Integer timeLimitMs;

    @NotNull
    @Min(16)
    private Integer memoryLimitMb;

    private String sampleInput;

    private String sampleOutput;

    private String testInput;

    private String expectedOutput;

    @NotNull
    @Min(0)
    @Max(1)
    private Integer status;
}
