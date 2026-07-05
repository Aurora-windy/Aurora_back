package com.aurora.hr.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 员工状态枚举
 *
 * <p>spec: docs/specs/2026-07-05-global-variables.md §4.1.3</p>
 * <p>hr_employee.status 字段。</p>
 */
@Getter
@AllArgsConstructor
@Schema(description = "员工状态：0离职 1在职 2试用 3休假")
public enum EmpStatusEnum {

    QUIT      (0, "离职"),
    ACTIVE    (1, "在职"),
    PROBATION (2, "试用"),
    LEAVE     (3, "休假");

    @JsonValue
    @Schema(description = "状态码")
    private final int code;

    @Schema(description = "描述")
    private final String label;

    public static EmpStatusEnum fromCode(int code) {
        for (EmpStatusEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }
}
