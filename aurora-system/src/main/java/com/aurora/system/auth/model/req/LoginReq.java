package com.aurora.system.auth.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 账号登录请求参数
 */
@Data
@Schema(description = "账号登录请求参数")
public class LoginReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户名", example = "admin")
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Schema(description = "密码（明文，开发期；上线前评估 RSA）", example = "admin123")
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "图形验证码（大小写不敏感）", example = "ABCD")
    @NotBlank(message = "验证码不能为空")
    private String captcha;

    @Schema(description = "验证码标识（来自 /auth/captcha 返回的 uuid）")
    @NotBlank(message = "验证码标识不能为空")
    private String uuid;
}
