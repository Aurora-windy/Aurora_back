package com.aurora.oj.problem.model.resp;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProblemResp {

    private Long id;

    private String title;

    private String description;

    private Integer difficulty;

    private Integer timeLimitMs;

    private Integer memoryLimitMb;

    private String sampleInput;

    private String sampleOutput;

    private String testInput;

    private String expectedOutput;

    private Integer status;

    private LocalDateTime createTime;
}
