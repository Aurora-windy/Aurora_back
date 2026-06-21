package com.aurora.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务错误码枚举
 *
 * <p>约定：1xx 系统 / 2xx 鉴权 / 3xx 用户 / 4xx 业务通用 / 5xx 模块业务</p>
 */
@Getter
@AllArgsConstructor
public enum BizCode {

    SUCCESS         (200, "操作成功"),

    // 1xx 系统
    SYSTEM_ERROR    (500, "系统异常"),
    PARAM_ERROR     (400, "参数校验失败"),
    BIZ_FAIL        (500, "业务处理失败"),

    // 2xx 鉴权
    UNAUTHORIZED    (401, "未登录或 Token 已过期"),
    FORBIDDEN       (403, "权限不足"),
    TOKEN_INVALID   (401, "Token 无效"),
    TOKEN_EXPIRED   (401, "Token 已过期"),
    CAPTCHA_ERROR   (500, "验证码错误或已过期"),

    // 3xx 用户
    USER_NOT_FOUND  (500, "用户不存在"),
    USER_PASSWORD_ERROR (500, "用户名或密码错误"),
    USER_DISABLED   (500, "账号已禁用"),

    // 4xx 业务通用
    DATA_NOT_FOUND  (500, "数据不存在"),
    DATA_EXISTS     (500, "数据已存在"),
    OPERATION_FAIL  (500, "操作失败");

    private final Integer code;
    private final String msg;
}
