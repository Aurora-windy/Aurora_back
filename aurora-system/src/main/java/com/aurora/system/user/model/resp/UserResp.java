package com.aurora.system.user.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户响应。
 */
@Data
@Builder
@Schema(description = "用户响应")
public class UserResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    private Integer gender;
    private Integer status;
    private Long deptId;
    private LocalDateTime lastLoginTime;
    private List<Long> roleIds;
    private List<String> roleCodes;
    private LocalDateTime createTime;
}
