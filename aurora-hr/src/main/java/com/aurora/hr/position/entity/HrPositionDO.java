package com.aurora.hr.position.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aurora.common.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 岗位实体，属于某个部门。
 *
 * <p>spec: docs/specs/2026-07-23-hr-core-org.md</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_position")
public class HrPositionDO extends BaseDO {

    private Long deptId;
    private String positionName;
    private String positionCode;
    private Integer sort;
    private Integer status;
}
