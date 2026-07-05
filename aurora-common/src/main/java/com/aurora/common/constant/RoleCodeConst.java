package com.aurora.common.constant;

/**
 * 角色编码常量
 *
 * <p>spec: docs/specs/2026-07-05-global-variables.md §4.2.3</p>
 * <p>sys_role.code 字段值来源（Liquibase seed 5 个角色）。</p>
 *
 * @see BizConst#ROLE_ADMIN 与本类 {@link #ADMIN} 等价，保留 BizConst.ROLE_ADMIN 是为兼容已有代码
 */
public final class RoleCodeConst {

    private RoleCodeConst() {
    }

    /** 系统管理员：全部数据权限（data_scope=1） */
    public static final String ADMIN = "admin";

    /** 人事管理员：本部门及下级数据权限（data_scope=2） */
    public static final String HR_ADMIN = "hr_admin";

    /** 教师：本人授课数据权限（data_scope=3） */
    public static final String EDU_TEACHER = "edu_teacher";

    /** 电商管理员：全部商品订单数据权限（data_scope=1） */
    public static final String MALL_ADMIN = "mall_admin";

    /** 学生：本人数据权限（data_scope=3），EDU/OJ/MALL/AI 通用 */
    public static final String STUDENT = "student";
}
