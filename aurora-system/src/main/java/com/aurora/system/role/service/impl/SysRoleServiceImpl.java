package com.aurora.system.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aurora.common.constant.RoleCodeConst;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.aurora.common.response.PageResult;
import com.aurora.system.role.entity.SysRoleDO;
import com.aurora.system.role.entity.SysRoleMenuDO;
import com.aurora.system.role.mapper.SysRoleMapper;
import com.aurora.system.role.mapper.SysRoleMenuMapper;
import com.aurora.system.role.model.req.RoleAddReq;
import com.aurora.system.role.model.req.RoleAssignMenuReq;
import com.aurora.system.role.model.req.RolePageReq;
import com.aurora.system.role.model.req.RoleStatusReq;
import com.aurora.system.role.model.req.RoleUpdateReq;
import com.aurora.system.role.model.resp.RoleResp;
import com.aurora.system.role.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 系统角色 Service 实现。
 */
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    @Override
    public PageResult<RoleResp> page(RolePageReq req) {
        LambdaQueryWrapper<SysRoleDO> wrapper = Wrappers.<SysRoleDO>lambdaQuery()
                .like(StringUtils.hasText(req.getName()), SysRoleDO::getName, req.getName())
                .like(StringUtils.hasText(req.getCode()), SysRoleDO::getCode, req.getCode())
                .eq(req.getStatus() != null, SysRoleDO::getStatus, req.getStatus())
                .orderByAsc(SysRoleDO::getSort)
                .orderByAsc(SysRoleDO::getId);
        Page<SysRoleDO> page = roleMapper.selectPage(
                new Page<>(req.normalizedPageNum(), req.normalizedPageSize()), wrapper);
        List<RoleResp> list = page.getRecords().stream().map(this::toResp).toList();
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public RoleResp detail(Long id) {
        return toResp(requireRole(id));
    }

    @Override
    public Long add(RoleAddReq req) {
        ensureCodeUnique(req.getCode(), null);
        SysRoleDO role = new SysRoleDO();
        role.setName(req.getName());
        role.setCode(req.getCode());
        role.setDataScope(req.getDataScope());
        role.setSort(req.getSort() == null ? 0 : req.getSort());
        role.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        role.setRemark(req.getRemark());
        roleMapper.insert(role);
        return role.getId();
    }

    @Override
    public void update(RoleUpdateReq req) {
        SysRoleDO existing = requireRole(req.getId());
        protectAdminRole(existing);
        ensureCodeUnique(req.getCode(), req.getId());
        SysRoleDO role = new SysRoleDO();
        role.setId(req.getId());
        role.setName(req.getName());
        role.setCode(req.getCode());
        role.setDataScope(req.getDataScope());
        role.setSort(req.getSort());
        role.setStatus(req.getStatus());
        role.setRemark(req.getRemark());
        roleMapper.updateById(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysRoleDO role = requireRole(id);
        protectAdminRole(role);
        if (roleMapper.countUsersByRoleId(id) > 0) {
            throw new BizException(BizCode.ROLE_HAS_USERS);
        }
        roleMapper.deleteById(id);
        roleMenuMapper.delete(Wrappers.<SysRoleMenuDO>lambdaQuery().eq(SysRoleMenuDO::getRoleId, id));
    }

    @Override
    public void updateStatus(RoleStatusReq req) {
        SysRoleDO role = requireRole(req.getId());
        if (RoleCodeConst.ADMIN.equals(role.getCode())) {
            throw new BizException(BizCode.OPERATION_FAIL.getCode(), "不允许禁用超级管理员角色");
        }
        SysRoleDO update = new SysRoleDO();
        update.setId(req.getId());
        update.setStatus(req.getStatus());
        roleMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenu(RoleAssignMenuReq req) {
        SysRoleDO role = requireRole(req.getRoleId());
        roleMenuMapper.delete(Wrappers.<SysRoleMenuDO>lambdaQuery().eq(SysRoleMenuDO::getRoleId, role.getId()));
        if (req.getMenuIds() == null || req.getMenuIds().isEmpty()) {
            return;
        }
        req.getMenuIds().stream().distinct()
                .forEach(menuId -> roleMenuMapper.insert(new SysRoleMenuDO(role.getId(), menuId)));
    }

    private SysRoleDO requireRole(Long id) {
        SysRoleDO role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException(BizCode.DATA_NOT_FOUND.getCode(), "角色不存在");
        }
        return role;
    }

    private void ensureCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<SysRoleDO> wrapper = Wrappers.<SysRoleDO>lambdaQuery()
                .eq(SysRoleDO::getCode, code)
                .ne(excludeId != null, SysRoleDO::getId, excludeId);
        if (roleMapper.selectCount(wrapper) > 0) {
            throw new BizException(BizCode.ROLE_CODE_DUPLICATED);
        }
    }

    private void protectAdminRole(SysRoleDO role) {
        if (RoleCodeConst.ADMIN.equals(role.getCode())) {
            throw new BizException(BizCode.OPERATION_FAIL.getCode(), "不允许修改或删除超级管理员角色");
        }
    }

    private RoleResp toResp(SysRoleDO role) {
        return RoleResp.builder()
                .id(role.getId())
                .name(role.getName())
                .code(role.getCode())
                .dataScope(role.getDataScope())
                .sort(role.getSort())
                .status(role.getStatus())
                .remark(role.getRemark())
                .menuIds(roleMapper.selectMenuIdsByRoleId(role.getId()))
                .createTime(role.getCreateTime())
                .build();
    }
}
