package com.aurora.edu.selection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aurora.common.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 选课记录 DO。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("edu_selection")
public class EduSelectionDO extends BaseDO {

    private Long studentId;
    private Long courseId;
    /** 选课状态：1已选 2已退 */
    private Integer status;
    private LocalDateTime selectedTime;
    private LocalDateTime droppedTime;
}
