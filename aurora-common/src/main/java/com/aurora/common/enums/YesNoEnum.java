package com.aurora.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通用是否枚举
 *
 * <p>spec: docs/specs/2026-07-05-global-variables.md §4.1.1</p>
 * <p>用于：是否默认、是否秒杀、是否样例等二元字段。</p>
 */
@Getter
@AllArgsConstructor
@Schema(description = "是否：0否 1是")
public enum YesNoEnum {

    NO (0, "否"),
    YES(1, "是");

    @JsonValue
    @Schema(description = "状态码")
    private final int code;

    @Schema(description = "描述")
    private final String label;

    public static YesNoEnum fromCode(int code) {
        for (YesNoEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return NO;
    }
}
