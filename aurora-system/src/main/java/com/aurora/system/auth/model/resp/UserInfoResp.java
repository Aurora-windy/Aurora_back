package com.aurora.system.auth.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 当前登录用户信息
 */
@Data
@Builder
@Schema(description = "当前登录用户信息")
public class UserInfoResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户 ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "角色编码列表（如 admin）")
    private List<String> roles;

    @Schema(description = "权限编码列表（Phase 1 后续 RBAC 实装后填充，本次为空数组占位）")
    private List<String> permissions;
}
