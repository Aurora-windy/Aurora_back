package com.aurora.common.enums.system;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据权限范围枚举
 *
 * <p>spec: docs/specs/2026-07-05-global-variables.md §4.1.2</p>
 * <p>sys_role.data_scope 字段；Service 层根据此值追加 SQL 条件。</p>
 */
@Getter
@AllArgsConstructor
@Schema(description = "数据范围：1全部 2本部门及下级 3本人")
public enum DataScopeEnum {

    ALL (1, "全部"),
    DEPT(2, "本部门及下级"),
    SELF(3, "本人");

    @JsonValue
    @Schema(description = "状态码")
    private final int code;

    @Schema(description = "描述")
    private final String label;
}
