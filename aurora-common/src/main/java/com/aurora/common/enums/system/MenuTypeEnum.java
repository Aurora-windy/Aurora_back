package com.aurora.common.enums.system;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 菜单类型枚举
 *
 * <p>spec: docs/specs/2026-07-05-global-variables.md §4.1.2</p>
 * <p>sys_menu.type 字段。</p>
 */
@Getter
@AllArgsConstructor
@Schema(description = "菜单类型：1目录 2菜单 3按钮")
public enum MenuTypeEnum {

    DIRECTORY(1, "目录"),
    MENU     (2, "菜单"),
    BUTTON   (3, "按钮");

    @JsonValue
    @Schema(description = "状态码")
    private final int code;

    @Schema(description = "描述")
    private final String label;

    public static MenuTypeEnum fromCode(int code) {
        for (MenuTypeEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }
}
