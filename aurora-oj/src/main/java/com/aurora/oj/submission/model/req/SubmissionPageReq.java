package com.aurora.oj.submission.model.req;

import com.aurora.common.base.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SubmissionPageReq extends PageRequest {

    private Long problemId;

    private Long userId;

    private Integer status;

    private String language;
}
