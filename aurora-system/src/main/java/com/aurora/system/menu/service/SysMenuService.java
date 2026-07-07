package com.aurora.system.menu.service;

import com.aurora.system.menu.model.req.MenuAddReq;
import com.aurora.system.menu.model.req.MenuUpdateReq;
import com.aurora.system.menu.model.resp.MenuResp;
import com.aurora.system.menu.model.resp.MenuTreeResp;
import com.aurora.system.menu.model.resp.UserRouteResp;

import java.util.List;

/**
 * 系统菜单 Service。
 */
public interface SysMenuService {

    List<MenuTreeResp> tree();

    MenuResp detail(Long id);

    Long add(MenuAddReq req);

    void update(MenuUpdateReq req);

    void delete(Long id);

    List<UserRouteResp> userRoutes();

    List<String> currentUserPermissions(Long userId);
}
