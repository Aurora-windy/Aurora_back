package com.aurora.system.role.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.aurora.system.role.entity.SysRoleDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统角色 Mapper。
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRoleDO> {

    @Select("""
            SELECT menu_id
              FROM sys_role_menu
             WHERE role_id = #{roleId}
             ORDER BY menu_id ASC
            """)
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    @Select("""
            SELECT COUNT(1)
              FROM sys_user_role
             WHERE role_id = #{roleId}
            """)
    long countUsersByRoleId(@Param("roleId") Long roleId);
}
