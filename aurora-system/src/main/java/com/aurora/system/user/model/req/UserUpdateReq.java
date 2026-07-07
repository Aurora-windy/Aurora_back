package com.aurora.system.user.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 用户修改请求。
 */
@Data
@Schema(description = "用户修改请求")
public class UserUpdateReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "用户 ID 不能为空")
    private Long id;

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    private String avatar;

    private String email;

    private String phone;

    private Integer gender;

    private Integer status;

    private Long deptId;

    private List<Long> roleIds;
}
