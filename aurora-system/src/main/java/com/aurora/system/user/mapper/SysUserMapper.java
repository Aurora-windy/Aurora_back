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
}
