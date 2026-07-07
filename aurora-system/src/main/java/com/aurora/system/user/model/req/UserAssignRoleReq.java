package com.aurora.system.user.model.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 用户分配角色请求。
 */
@Data
public class UserAssignRoleReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    private List<Long> roleIds;
}
