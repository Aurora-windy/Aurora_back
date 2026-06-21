package com.aurora.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一返回格式（SRS §5.1）
 *
 * <pre>
 * code:
 *   200  成功
 *   400  参数错误
 *   401  未登录 / Token 过期
 *   403  权限不足
 *   500  业务失败 / 系统异常
 * </pre>
 */
@Data
@Schema(description = "统一返回格式")
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "状态码：200成功 400参数错误 401未登录 403权限不足 500业务失败")
    private Integer code;

    @Schema(description = "提示信息")
    private String msg;

    @Schema(description = "业务数据，无数据时为 null")
    private T data;

    public Result() {
    }

    public Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> Result<T> ok() {
        return new Result<>(BizCode.SUCCESS.getCode(), "操作成功", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(BizCode.SUCCESS.getCode(), "操作成功", data);
    }

    public static <T> Result<T> ok(String msg, T data) {
        return new Result<>(BizCode.SUCCESS.getCode(), msg, data);
    }

    public static <T> Result<T> fail(String msg) {
        return new Result<>(BizCode.BIZ_FAIL.getCode(), msg, null);
    }

    public static <T> Result<T> fail(BizCode bizCode) {
        return new Result<>(bizCode.getCode(), bizCode.getMsg(), null);
    }

    public static <T> Result<T> fail(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }

    public boolean isSuccess() {
        return BizCode.SUCCESS.getCode().equals(this.code);
    }
}
