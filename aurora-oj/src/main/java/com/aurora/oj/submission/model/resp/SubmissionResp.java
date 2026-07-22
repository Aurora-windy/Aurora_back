package com.aurora.oj.submission.model.resp;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SubmissionResp {

    private Long id;

    private Long problemId;

    private String problemTitle;

    private Long userId;

    private String language;

    private String sourceCode;

    private Integer status;

    private Integer timeUsedMs;

    private Integer memoryUsedKb;

    private String errorMessage;

    private LocalDateTime createTime;
}
