package com.aurora.system.user.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 用户新增请求。
 */
@Data
@Schema(description = "用户新增请求")
public class UserAddReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "用户名不能为空")
    private String username;

    private String password;

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    private String avatar;

    private String email;

    private String phone;

    private Integer gender = 0;

    private Integer status = 1;

    private Long deptId;

    private List<Long> roleIds;
}
