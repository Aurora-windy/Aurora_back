package com.aurora.system.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.aurora.system.user.entity.SysUserRoleDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户-角色关系 Mapper。
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRoleDO> {
}
