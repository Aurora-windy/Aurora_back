package com.aurora.system.role.model.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 角色启停请求。
 */
@Data
public class RoleStatusReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "角色 ID 不能为空")
    private Long id;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
