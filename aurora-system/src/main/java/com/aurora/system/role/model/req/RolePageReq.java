package com.aurora.system.role.model.req;

import com.aurora.common.base.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色分页查询请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "角色分页查询请求")
public class RolePageReq extends PageRequest {

    private String name;

    private String code;

    private Integer status;
}
