package com.aurora.system.role.model.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 角色分配菜单请求。
 */
@Data
public class RoleAssignMenuReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "角色 ID 不能为空")
    private Long roleId;

    private List<Long> menuIds;
}
