package com.aurora.system.user.service;

import com.aurora.common.response.PageResult;
import com.aurora.system.user.model.req.BatchIdsReq;
import com.aurora.system.user.model.req.UserAddReq;
import com.aurora.system.user.model.req.UserAssignRoleReq;
import com.aurora.system.user.model.req.UserPageReq;
import com.aurora.system.user.model.req.UserResetPasswordReq;
import com.aurora.system.user.model.req.UserStatusReq;
import com.aurora.system.user.model.req.UserUpdateReq;
import com.aurora.system.user.model.resp.UserResp;

/**
 * 系统用户 Service。
 */
public interface SysUserService {

    PageResult<UserResp> page(UserPageReq req);

    UserResp detail(Long id);

    Long add(UserAddReq req);

    void update(UserUpdateReq req);

    void delete(Long id);

    void deleteBatch(BatchIdsReq req);

    void updateStatus(UserStatusReq req);

    void resetPassword(UserResetPasswordReq req);

    void assignRole(UserAssignRoleReq req);
}
