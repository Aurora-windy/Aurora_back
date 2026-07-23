package com.aurora.hr.employee.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aurora.common.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 员工档案实体。
 *
 * <p>spec: docs/specs/2026-07-23-hr-core-org.md</p>
 * <p>user_id 预留可空，本期不消费；emp_no 全局唯一。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_employee")
public class HrEmployeeDO extends BaseDO {

    private String empNo;
    private Long userId;
    private Long deptId;
    private Long positionId;
    private String name;
    private Integer gender;
    private String phone;
    private LocalDate entryDate;
    private Integer status;
}
