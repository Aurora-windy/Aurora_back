package com.aurora.common.exception;

import com.aurora.common.response.BizCode;
import lombok.Getter;

/**
 * 业务异常 - 所有可预期的业务错误统一抛此异常
 *
 * <p>由 {@link GlobalExceptionHandler} 捕获并转为统一返回格式。</p>
 */
@Getter
public class BizException extends RuntimeException {

    private final Integer code;

    public BizException(String message) {
        super(message);
        this.code = BizCode.BIZ_FAIL.getCode();
    }

    public BizException(BizCode bizCode) {
        super(bizCode.getMsg());
        this.code = bizCode.getCode();
    }

    public BizException(BizCode bizCode, String message) {
        super(message);
        this.code = bizCode.getCode();
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
