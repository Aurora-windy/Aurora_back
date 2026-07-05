package com.aurora.common.enums.system;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 登录方式枚举
 *
 * <p>spec: docs/specs/2026-07-05-global-variables.md §4.1.2</p>
 * <p>本期仅 ACCOUNT 实装，SMS / OAUTH 占位。</p>
 */
@Getter
@AllArgsConstructor
@Schema(description = "登录方式：1账号 2短信 3OAuth")
public enum LoginTypeEnum {

    ACCOUNT(1, "账号"),
    SMS    (2, "短信"),
    OAUTH  (3, "OAuth");

    @JsonValue
    @Schema(description = "状态码")
    private final int code;

    @Schema(description = "描述")
    private final String label;
}
