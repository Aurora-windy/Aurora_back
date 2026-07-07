package com.aurora.system.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aurora.common.constant.BizConst;
import com.aurora.common.constant.RoleCodeConst;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.aurora.common.response.PageResult;
import com.aurora.system.user.entity.SysUserDO;
import com.aurora.system.user.entity.SysUserRoleDO;
import com.aurora.system.user.mapper.SysUserMapper;
import com.aurora.system.user.mapper.SysUserRoleMapper;
import com.aurora.system.user.model.req.BatchIdsReq;
import com.aurora.system.user.model.req.UserAddReq;
import com.aurora.system.user.model.req.UserAssignRoleReq;
import com.aurora.system.user.model.req.UserPageReq;
import com.aurora.system.user.model.req.UserResetPasswordReq;
import com.aurora.system.user.model.req.UserStatusReq;
import com.aurora.system.user.model.req.UserUpdateReq;
import com.aurora.system.user.model.resp.UserResp;
import com.aurora.system.user.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 系统用户 Service 实现。
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(BizConst.BCRYPT_STRENGTH);

    @Override
    public PageResult<UserResp> page(UserPageReq req) {
        LambdaQueryWrapper<SysUserDO> wrapper = Wrappers.<SysUserDO>lambdaQuery()
                .like(StringUtils.hasText(req.getUsername()), SysUserDO::getUsername, req.getUsername())
                .like(StringUtils.hasText(req.getNickname()), SysUserDO::getNickname, req.getNickname())
                .eq(req.getStatus() != null, SysUserDO::getStatus, req.getStatus())
                .orderByDesc(SysUserDO::getCreateTime);
        Page<SysUserDO> page = userMapper.selectPage(
                new Page<>(req.normalizedPageNum(), req.normalizedPageSize()), wrapper);
        List<UserResp> list = page.getRecords().stream().map(this::toResp).toList();
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public UserResp detail(Long id) {
        SysUserDO user = requireUser(id);
        return toResp(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(UserAddReq req) {
        ensureUsernameUnique(req.getUsername(), null);
        SysUserDO user = new SysUserDO();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(
                StringUtils.hasText(req.getPassword()) ? req.getPassword() : BizConst.DEFAULT_PASSWORD));
        user.setNickname(req.getNickname());
        user.setAvatar(req.getAvatar());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setGender(req.getGender());
        user.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        user.setDeptId(req.getDeptId());
        userMapper.insert(user);
        saveUserRoles(user.getId(), req.getRoleIds());
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(UserUpdateReq req) {
        requireUser(req.getId());
        ensureUsernameUnique(req.getUsername(), req.getId());
        SysUserDO user = new SysUserDO();
        user.setId(req.getId());
        user.setUsername(req.getUsername());
        user.setNickname(req.getNickname());
        user.setAvatar(req.getAvatar());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setGender(req.getGender());
        user.setStatus(req.getStatus());
        user.setDeptId(req.getDeptId());
        userMapper.updateById(user);
        if (req.getRoleIds() != null) {
            saveUserRoles(req.getId(), req.getRoleIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysUserDO user = requireUser(id);
        protectLastAdmin(user.getId());
        userMapper.deleteById(id);
        userRoleMapper.delete(Wrappers.<SysUserRoleDO>lambdaQuery().eq(SysUserRoleDO::getUserId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(BatchIdsReq req) {
        for (Long id : req.getIds()) {
            delete(id);
        }
    }

    @Override
    public void updateStatus(UserStatusReq req) {
        SysUserDO user = requireUser(req.getId());
        if (req.getStatus() != null && req.getStatus() == 0) {
            protectLastAdmin(user.getId());
        }
        SysUserDO update = new SysUserDO();
        update.setId(req.getId());
        update.setStatus(req.getStatus());
        userMapper.updateById(update);
    }

    @Override
    public void resetPassword(UserResetPasswordReq req) {
        requireUser(req.getId());
        SysUserDO update = new SysUserDO();
        update.setId(req.getId());
        update.setPassword(passwordEncoder.encode(
                StringUtils.hasText(req.getNewPassword()) ? req.getNewPassword() : BizConst.DEFAULT_PASSWORD));
        userMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRole(UserAssignRoleReq req) {
        requireUser(req.getUserId());
        saveUserRoles(req.getUserId(), req.getRoleIds());
    }

    private SysUserDO requireUser(Long id) {
        SysUserDO user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(BizCode.USER_NOT_FOUND);
        }
        return user;
    }

    private void ensureUsernameUnique(String username, Long excludeId) {
        LambdaQueryWrapper<SysUserDO> wrapper = Wrappers.<SysUserDO>lambdaQuery()
                .eq(SysUserDO::getUsername, username)
                .ne(excludeId != null, SysUserDO::getId, excludeId);
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BizException(BizCode.USERNAME_DUPLICATED);
        }
    }

    private void saveUserRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(Wrappers.<SysUserRoleDO>lambdaQuery().eq(SysUserRoleDO::getUserId, userId));
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        roleIds.stream().distinct().forEach(roleId -> userRoleMapper.insert(new SysUserRoleDO(userId, roleId)));
    }

    private void protectLastAdmin(Long userId) {
        List<String> roles = userMapper.selectRoleCodesByUserId(userId);
        if (roles.contains(RoleCodeConst.ADMIN)
                && userMapper.countEnabledUsersByRoleCode(RoleCodeConst.ADMIN) <= 1) {
            throw new BizException(BizCode.LAST_ADMIN_PROTECT);
        }
    }

    private UserResp toResp(SysUserDO user) {
        return UserResp.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .email(user.getEmail())
                .phone(user.getPhone())
                .gender(user.getGender())
                .status(user.getStatus())
                .deptId(user.getDeptId())
                .lastLoginTime(user.getLastLoginTime())
                .roleIds(userMapper.selectRoleIdsByUserId(user.getId()))
                .roleCodes(userMapper.selectRoleCodesByUserId(user.getId()))
                .createTime(user.getCreateTime())
                .build();
    }
}
