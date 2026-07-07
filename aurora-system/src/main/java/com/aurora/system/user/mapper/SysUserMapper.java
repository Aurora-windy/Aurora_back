package com.aurora.system.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.aurora.system.user.entity.SysUserDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统用户 Mapper
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUserDO> {

    /**
     * 根据用户 ID 查询所拥有的角色编码列表
     *
     * <p>JOIN sys_user_role + sys_role，过滤已逻辑删除与禁用的角色。</p>
     */
    @Select("""
            SELECT r.code
              FROM sys_role r
              JOIN sys_user_role ur ON ur.role_id = r.id
             WHERE ur.user_id = #{userId}
               AND r.deleted = 0
               AND r.status = 1
            """)
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT r.id
              FROM sys_role r
              JOIN sys_user_role ur ON ur.role_id = r.id
             WHERE ur.user_id = #{userId}
               AND r.deleted = 0
             ORDER BY r.sort ASC, r.id ASC
            """)
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT m.permission
              FROM sys_menu m
              JOIN sys_role_menu rm ON rm.menu_id = m.id
              JOIN sys_user_role ur ON ur.role_id = rm.role_id
              JOIN sys_role r ON r.id = ur.role_id
             WHERE ur.user_id = #{userId}
               AND r.deleted = 0
               AND r.status = 1
               AND m.deleted = 0
               AND m.status = 1
               AND m.permission IS NOT NULL
               AND m.permission <> ''
             ORDER BY m.permission ASC
            """)
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT permission
              FROM sys_menu
             WHERE deleted = 0
               AND status = 1
               AND permission IS NOT NULL
               AND permission <> ''
             ORDER BY permission ASC
            """)
    List<String> selectAllPermissionCodes();

    @Select("""
            SELECT COUNT(1)
              FROM sys_user u
              JOIN sys_user_role ur ON ur.user_id = u.id
              JOIN sys_role r ON r.id = ur.role_id
             WHERE r.code = #{roleCode}
               AND u.deleted = 0
               AND u.status = 1
               AND r.deleted = 0
               AND r.status = 1
            """)
    long countEnabledUsersByRoleCode(@Param("roleCode") String roleCode);
}
