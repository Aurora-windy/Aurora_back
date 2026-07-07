package com.aurora.system.role.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 角色新增请求。
 */
@Data
public class RoleAddReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "角色名称不能为空")
    private String name;

    @NotBlank(message = "角色编码不能为空")
    private String code;

    @NotNull(message = "数据权限范围不能为空")
    private Integer dataScope;

    private Integer sort = 0;

    private Integer status = 1;

    private String remark;
}
