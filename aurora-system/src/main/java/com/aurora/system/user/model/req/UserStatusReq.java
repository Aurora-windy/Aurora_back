package com.aurora.system.user.model.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户启停请求。
 */
@Data
public class UserStatusReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "用户 ID 不能为空")
    private Long id;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
