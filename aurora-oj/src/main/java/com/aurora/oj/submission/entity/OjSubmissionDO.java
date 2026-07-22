package com.aurora.oj.submission.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aurora.common.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oj_submission")
public class OjSubmissionDO extends BaseDO {

    private Long problemId;

    private Long userId;

    private Integer language;

    private String sourceCode;

    private Integer status;

    private Integer timeUsedMs;

    private Integer memoryUsedKb;

    private String errorMessage;
}
