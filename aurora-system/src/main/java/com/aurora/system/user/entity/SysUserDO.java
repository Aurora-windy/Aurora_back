package com.aurora.system.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aurora.common.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 系统用户 DO（对应 sys_user 表）
 *
 * <p>Phase 1 登录最小闭环仅使用 username/password/nickname/avatar/status/last_login_time 字段。
 * dept_id/email/phone/gender 等字段在后续 Phase 1 用户管理 CRUD 时再展开使用。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUserDO extends BaseDO {

    private String username;

    private String password;

    private String nickname;

    private String avatar;

    private String email;

    private String phone;

    private Integer gender;

    /** 状态：1启用 0禁用 */
    private Integer status;

    private Long deptId;

    private LocalDateTime lastLoginTime;
}
