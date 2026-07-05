package com.aurora.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 性别枚举
 *
 * <p>spec: docs/specs/2026-07-05-global-variables.md §4.1.1</p>
 */
@Getter
@AllArgsConstructor
@Schema(description = "性别：0未知 1男 2女")
public enum GenderEnum {

    UNKNOWN(0, "未知"),
    MALE  (1, "男"),
    FEMALE(2, "女");

    @JsonValue
    @Schema(description = "状态码")
    private final int code;

    @Schema(description = "描述")
    private final String label;

    public static GenderEnum fromCode(int code) {
        for (GenderEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return UNKNOWN;
    }
}
