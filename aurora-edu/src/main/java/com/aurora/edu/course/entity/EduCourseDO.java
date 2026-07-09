package com.aurora.edu.course.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aurora.common.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程 DO。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("edu_course")
public class EduCourseDO extends BaseDO {

    private String courseCode;
    private String name;
    private Long teacherId;
    /** 课程类别：1必修 2选修 3公选 */
    private Integer category;
    private BigDecimal credit;
    private Integer capacity;
    private Integer selectedCount;
    private LocalDateTime selectionStartTime;
    private LocalDateTime selectionEndTime;
    /** 状态：1启用 0禁用 */
    private Integer status;
}
