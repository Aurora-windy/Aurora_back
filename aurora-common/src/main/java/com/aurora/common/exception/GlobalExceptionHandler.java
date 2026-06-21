package com.aurora.common.exception;

import com.aurora.common.response.BizCode;
import com.aurora.common.response.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * <p>SRS §5.2: 后端全局异常捕获，任何代码报错不导致服务宕机。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BizException.class)
    public Result<?> handleBiz(BizException e, HttpServletRequest req) {
        log.warn("[业务异常] {} {} -> {}", req.getMethod(), req.getRequestURI(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验失败 - @Valid @RequestBody
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        log.warn("[参数校验失败] {}", msg);
        return Result.fail(BizCode.PARAM_ERROR.getCode(), msg);
    }

    /**
     * 参数校验失败 - @ModelAttribute form bind
     */
    @ExceptionHandler(BindException.class)
    public Result<?> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        log.warn("[参数绑定失败] {}", msg);
        return Result.fail(BizCode.PARAM_ERROR.getCode(), msg);
    }

    /**
     * 缺少必填参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<?> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("[缺少参数] {}", e.getParameterName());
        return Result.fail(BizCode.PARAM_ERROR.getCode(), "缺少必填参数: " + e.getParameterName());
    }

    /**
     * 请求体不可读（JSON 格式错误）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<?> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("[请求体解析失败] {}", e.getMessage());
        return Result.fail(BizCode.PARAM_ERROR.getCode(), "请求体格式错误");
    }

    /**
     * HTTP 方法不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<?> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("[方法不支持] {}", e.getMessage());
        return Result.fail(BizCode.OPERATION_FAIL.getCode(), "请求方法不支持: " + e.getMethod());
    }

    /**
     * 兜底异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e, HttpServletRequest req) {
        log.error("[系统异常] {} {}", req.getMethod(), req.getRequestURI(), e);
        return Result.fail(BizCode.SYSTEM_ERROR.getCode(),
                BizCode.SYSTEM_ERROR.getMsg() + ": " + e.getMessage());
    }

    private String formatFieldError(FieldError f) {
        return f.getField() + ": " + f.getDefaultMessage();
    }
}
