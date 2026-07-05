package com.aurora.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通用启停状态枚举
 *
 * <p>spec: docs/specs/2026-07-05-global-variables.md §4.1.1</p>
 * <p>用于：用户、角色、商品、岗位、菜单等需要"启用/禁用"的场景。</p>
 */
@Getter
@AllArgsConstructor
@Schema(description = "状态：0禁用 1启用")
public enum StatusEnum {

    DISABLED(0, "禁用"),
    ENABLED (1, "启用");

    @JsonValue
    @Schema(description = "状态码")
    private final int code;

    @Schema(description = "描述")
    private final String label;

    public static StatusEnum fromCode(int code) {
        for (StatusEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return DISABLED;
    }
}
