package com.aurora.system.auth.service.impl;

import cn.dev33.satoken.stp.StpInterface;
import com.aurora.common.constant.RoleCodeConst;
import com.aurora.system.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sa-Token 角色/权限数据源。
 */
@Component
@RequiredArgsConstructor
public class SaPermissionServiceImpl implements StpInterface {

    private final SysUserMapper userMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = Long.valueOf(String.valueOf(loginId));
        List<String> roles = userMapper.selectRoleCodesByUserId(userId);
        if (roles.contains(RoleCodeConst.ADMIN)) {
            return List.of("*");
        }
        return userMapper.selectPermissionCodesByUserId(userId);
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return userMapper.selectRoleCodesByUserId(Long.valueOf(String.valueOf(loginId)));
    }
}
