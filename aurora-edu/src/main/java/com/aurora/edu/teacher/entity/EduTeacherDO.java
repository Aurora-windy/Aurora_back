package com.aurora.edu.teacher.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aurora.common.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 教师档案 DO。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("edu_teacher")
public class EduTeacherDO extends BaseDO {

    private Long userId;
    private String teacherNo;
    private String name;
    /** 职称：1教授 2副教授 3讲师 4助教 */
    private Integer title;
    private String phone;
    private String college;
    /** 状态：1启用 0禁用 */
    private Integer status;
}
