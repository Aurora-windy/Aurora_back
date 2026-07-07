package com.aurora.system.role.service;

import com.aurora.common.response.PageResult;
import com.aurora.system.role.model.req.RoleAddReq;
import com.aurora.system.role.model.req.RoleAssignMenuReq;
import com.aurora.system.role.model.req.RolePageReq;
import com.aurora.system.role.model.req.RoleStatusReq;
import com.aurora.system.role.model.req.RoleUpdateReq;
import com.aurora.system.role.model.resp.RoleResp;

/**
 * 系统角色 Service。
 */
public interface SysRoleService {

    PageResult<RoleResp> page(RolePageReq req);

    RoleResp detail(Long id);

    Long add(RoleAddReq req);

    void update(RoleUpdateReq req);

    void delete(Long id);

    void updateStatus(RoleStatusReq req);

    void assignMenu(RoleAssignMenuReq req);
}
