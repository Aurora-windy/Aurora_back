package com.aurora.system.user.model.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 重置用户密码请求。
 */
@Data
public class UserResetPasswordReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "用户 ID 不能为空")
    private Long id;

    private String newPassword;
}
