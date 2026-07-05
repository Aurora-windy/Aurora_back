package com.aurora.edu.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 学籍状态枚举
 *
 * <p>spec: docs/specs/2026-07-05-global-variables.md §4.1.4</p>
 * <p>edu_student.status 字段。</p>
 */
@Getter
@AllArgsConstructor
@Schema(description = "学籍状态：1在读 2休学 3毕业 4退学")
public enum StudentStatusEnum {

    IN_SCHOOL (1, "在读"),
    SUSPENDED (2, "休学"),
    GRADUATED (3, "毕业"),
    DROPPED   (4, "退学");

    @JsonValue
    @Schema(description = "状态码")
    private final int code;

    @Schema(description = "描述")
    private final String label;

    public static StudentStatusEnum fromCode(int code) {
        for (StudentStatusEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }
}
