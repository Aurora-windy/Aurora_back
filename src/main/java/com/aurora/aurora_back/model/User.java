package com.aurora.aurora_back.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    /**
     * 主键ID（bigint → Long）
     */
    private Long id;

    /**
     * 用户名（varchar(256) → String）
     */
    private String username;

    /**
     * 账号（varchar(256) → String）
     */
    private String account;

    /**
     * 头像图片（varchar(1024) → String）
     */
    private String headImage;

    /**
     * 性别（tinyint → Integer，0/1/2）
     */
    private Integer gender;

    /**
     * 密码（varchar(512) → String）
     */
    private String password;

    /**
     * 邮箱（varchar(512) → String）
     */
    private String email;

    /**
     * 状态（int → Integer，默认0）
     */
    private Integer status;

    /**
     * 创建时间（datetime → LocalDateTime）
     */
    private LocalDateTime createTime;

    /**
     * 更新时间（datetime → LocalDateTime）
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除（tinyint → Integer，默认0）
     */
    private Integer isDelete;

    /**
     * 手机号（varchar → String）
     */
    private String phone;

    /**
     * 头像地址（varchar → String）
     */
    private String avatar;

    /**
     * 用户角色（varchar → String）
     */
    private String role;
}