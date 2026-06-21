package com.aurora.common.base;

import com.aurora.common.response.Result;

/**
 * Controller 基类
 *
 * <p>提供常用返回方法包装，业务 Controller 可继承此类避免重复 import。</p>
 */
public abstract class BaseController {

    protected <T> Result<T> ok() {
        return Result.ok();
    }

    protected <T> Result<T> ok(T data) {
        return Result.ok(data);
    }

    protected <T> Result<T> ok(String msg, T data) {
        return Result.ok(msg, data);
    }

    protected <T> Result<T> fail(String msg) {
        return Result.fail(msg);
    }
}
