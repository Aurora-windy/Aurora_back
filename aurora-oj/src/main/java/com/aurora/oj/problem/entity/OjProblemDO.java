package com.aurora.oj.problem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aurora.common.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oj_problem")
public class OjProblemDO extends BaseDO {

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
}
