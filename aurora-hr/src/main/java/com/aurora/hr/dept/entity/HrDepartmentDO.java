package com.aurora.hr.dept.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aurora.common.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 部门实体（树形，parent_id 自引用）。
 *
 * <p>spec: docs/specs/2026-07-23-hr-core-org.md</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_department")
public class HrDepartmentDO extends BaseDO {

    private Long parentId;
    private String deptName;
    private String deptCode;
    private Integer sort;
    private Integer status;
}
