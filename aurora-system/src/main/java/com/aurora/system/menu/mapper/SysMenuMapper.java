package com.aurora.system.menu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.aurora.system.menu.entity.SysMenuDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统菜单 Mapper。
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenuDO> {

    @Select("""
            SELECT DISTINCT m.*
              FROM sys_menu m
              JOIN sys_role_menu rm ON rm.menu_id = m.id
              JOIN sys_user_role ur ON ur.role_id = rm.role_id
              JOIN sys_role r ON r.id = ur.role_id
             WHERE ur.user_id = #{userId}
               AND r.deleted = 0
               AND r.status = 1
               AND m.deleted = 0
               AND m.status = 1
             ORDER BY m.sort ASC, m.id ASC
            """)
    List<SysMenuDO> selectMenusByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT *
              FROM sys_menu
             WHERE deleted = 0
               AND status = 1
             ORDER BY sort ASC, id ASC
            """)
    List<SysMenuDO> selectAllEnabledMenus();

    @Select("""
            SELECT COUNT(1)
              FROM sys_menu
             WHERE parent_id = #{parentId}
               AND deleted = 0
            """)
    long countChildren(@Param("parentId") Long parentId);
}
