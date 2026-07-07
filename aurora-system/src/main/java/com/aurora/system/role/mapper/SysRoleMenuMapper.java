package com.aurora.system.role.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.aurora.system.role.entity.SysRoleMenuDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色-菜单关系 Mapper。
 */
@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenuDO> {
}
