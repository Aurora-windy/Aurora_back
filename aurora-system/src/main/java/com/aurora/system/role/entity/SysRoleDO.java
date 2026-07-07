package com.aurora.system.role.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aurora.common.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统角色 DO。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRoleDO extends BaseDO {

    private String name;

    private String code;

    private Integer dataScope;

    private Integer sort;

    private Integer status;

    private String remark;
}
