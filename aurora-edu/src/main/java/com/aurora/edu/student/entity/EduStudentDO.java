package com.aurora.edu.student.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.aurora.common.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 学生档案 DO。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("edu_student")
public class EduStudentDO extends BaseDO {

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long userId;
    private String studentNo;
    private String name;
    private Integer gender;
    private String phone;
    private String major;
    private String className;
    /** 学籍状态：1在读 2休学 3毕业 4退学 */
    private Integer status;
}
