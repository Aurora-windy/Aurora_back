package com.aurora.system.auth.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登录响应
 */
@Data
@Builder
@Schema(description = "登录响应")
public class LoginResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "访问令牌（前端存 localStorage，请求头 Authorization: Bearer xxx）")
    private String token;

    @Schema(description = "用户 ID")
    private Long userId;
}
