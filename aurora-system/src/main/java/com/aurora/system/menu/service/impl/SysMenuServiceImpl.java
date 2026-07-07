package com.aurora.system.menu.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.aurora.common.constant.RoleCodeConst;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.aurora.common.util.SecurityUtil;
import com.aurora.system.menu.entity.SysMenuDO;
import com.aurora.system.menu.mapper.SysMenuMapper;
import com.aurora.system.menu.model.req.MenuAddReq;
import com.aurora.system.menu.model.req.MenuUpdateReq;
import com.aurora.system.menu.model.resp.MenuResp;
import com.aurora.system.menu.model.resp.MenuTreeResp;
import com.aurora.system.menu.model.resp.UserRouteResp;
import com.aurora.system.menu.service.SysMenuService;
import com.aurora.system.role.entity.SysRoleMenuDO;
import com.aurora.system.role.mapper.SysRoleMenuMapper;
import com.aurora.system.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 系统菜单 Service 实现。
 */
@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl implements SysMenuService {

    private static final int TYPE_BUTTON = 3;

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserMapper userMapper;

    @Override
    public List<MenuTreeResp> tree() {
        List<SysMenuDO> menus = menuMapper.selectList(Wrappers.<SysMenuDO>lambdaQuery()
                .orderByAsc(SysMenuDO::getSort)
                .orderByAsc(SysMenuDO::getId));
        return buildMenuTree(menus);
    }

    @Override
    public MenuResp detail(Long id) {
        return toResp(requireMenu(id));
    }

    @Override
    public Long add(MenuAddReq req) {
        SysMenuDO menu = new SysMenuDO();
        menu.setParentId(req.getParentId() == null ? 0L : req.getParentId());
        menu.setTitle(req.getTitle());
        menu.setType(req.getType());
        menu.setName(req.getName());
        menu.setPath(req.getPath());
        menu.setComponent(req.getComponent());
        menu.setIcon(req.getIcon());
        menu.setPermission(req.getPermission());
        menu.setSort(req.getSort() == null ? 0 : req.getSort());
        menu.setVisible(req.getVisible() == null ? 1 : req.getVisible());
        menu.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        menuMapper.insert(menu);
        return menu.getId();
    }

    @Override
    public void update(MenuUpdateReq req) {
        requireMenu(req.getId());
        if (Objects.equals(req.getId(), req.getParentId())) {
            throw new BizException(BizCode.OPERATION_FAIL.getCode(), "上级菜单不能选择自己");
        }
        SysMenuDO menu = new SysMenuDO();
        menu.setId(req.getId());
        menu.setParentId(req.getParentId() == null ? 0L : req.getParentId());
        menu.setTitle(req.getTitle());
        menu.setType(req.getType());
        menu.setName(req.getName());
        menu.setPath(req.getPath());
        menu.setComponent(req.getComponent());
        menu.setIcon(req.getIcon());
        menu.setPermission(req.getPermission());
        menu.setSort(req.getSort());
        menu.setVisible(req.getVisible());
        menu.setStatus(req.getStatus());
        menuMapper.updateById(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireMenu(id);
        if (menuMapper.countChildren(id) > 0) {
            throw new BizException(BizCode.OPERATION_FAIL.getCode(), "该菜单下有子菜单，无法删除");
        }
        menuMapper.deleteById(id);
        roleMenuMapper.delete(Wrappers.<SysRoleMenuDO>lambdaQuery().eq(SysRoleMenuDO::getMenuId, id));
    }

    @Override
    public List<UserRouteResp> userRoutes() {
        Long userId = SecurityUtil.requireUserId();
        List<String> roles = userMapper.selectRoleCodesByUserId(userId);
        List<SysMenuDO> menus = roles.contains(RoleCodeConst.ADMIN)
                ? menuMapper.selectAllEnabledMenus()
                : menuMapper.selectMenusByUserId(userId);
        List<SysMenuDO> routes = menus.stream()
                .filter(menu -> !Objects.equals(menu.getType(), TYPE_BUTTON))
                .filter(menu -> Objects.equals(menu.getVisible(), 1))
                .sorted(Comparator.comparing(SysMenuDO::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SysMenuDO::getId))
                .toList();
        return buildRouteTree(routes);
    }

    @Override
    public List<String> currentUserPermissions(Long userId) {
        List<String> roles = userMapper.selectRoleCodesByUserId(userId);
        if (roles.contains(RoleCodeConst.ADMIN)) {
            return userMapper.selectAllPermissionCodes();
        }
        return userMapper.selectPermissionCodesByUserId(userId);
    }

    private SysMenuDO requireMenu(Long id) {
        SysMenuDO menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BizException(BizCode.DATA_NOT_FOUND.getCode(), "菜单不存在");
        }
        return menu;
    }

    private List<MenuTreeResp> buildMenuTree(List<SysMenuDO> menus) {
        Map<Long, MenuTreeResp> map = new LinkedHashMap<>();
        for (SysMenuDO menu : menus) {
            map.put(menu.getId(), toTreeResp(menu));
        }
        List<MenuTreeResp> roots = new ArrayList<>();
        for (SysMenuDO menu : menus) {
            MenuTreeResp node = map.get(menu.getId());
            MenuTreeResp parent = map.get(menu.getParentId());
            if (parent == null || Objects.equals(menu.getParentId(), 0L)) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    private List<UserRouteResp> buildRouteTree(List<SysMenuDO> menus) {
        Map<Long, UserRouteResp> map = new LinkedHashMap<>();
        for (SysMenuDO menu : menus) {
            map.put(menu.getId(), toRouteResp(menu));
        }
        List<UserRouteResp> roots = new ArrayList<>();
        for (SysMenuDO menu : menus) {
            UserRouteResp node = map.get(menu.getId());
            UserRouteResp parent = map.get(menu.getParentId());
            if (parent == null || Objects.equals(menu.getParentId(), 0L)) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    private MenuResp toResp(SysMenuDO menu) {
        return MenuResp.builder()
                .id(menu.getId())
                .parentId(menu.getParentId())
                .title(menu.getTitle())
                .type(menu.getType())
                .name(menu.getName())
                .path(menu.getPath())
                .component(menu.getComponent())
                .icon(menu.getIcon())
                .permission(menu.getPermission())
                .sort(menu.getSort())
                .visible(menu.getVisible())
                .status(menu.getStatus())
                .createTime(menu.getCreateTime())
                .build();
    }

    private MenuTreeResp toTreeResp(SysMenuDO menu) {
        return MenuTreeResp.builder()
                .id(menu.getId())
                .parentId(menu.getParentId())
                .title(menu.getTitle())
                .type(menu.getType())
                .name(menu.getName())
                .path(menu.getPath())
                .component(menu.getComponent())
                .icon(menu.getIcon())
                .permission(menu.getPermission())
                .sort(menu.getSort())
                .visible(menu.getVisible())
                .status(menu.getStatus())
                .build();
    }

    private UserRouteResp toRouteResp(SysMenuDO menu) {
        return UserRouteResp.builder()
                .id(menu.getId())
                .parentId(menu.getParentId())
                .title(menu.getTitle())
                .name(menu.getName())
                .path(menu.getPath())
                .component(menu.getComponent())
                .icon(menu.getIcon())
                .type(menu.getType())
                .sort(menu.getSort())
                .build();
    }
}
