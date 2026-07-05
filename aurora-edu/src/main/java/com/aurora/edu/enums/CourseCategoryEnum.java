package com.aurora.edu.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 课程类别枚举
 *
 * <p>spec: docs/specs/2026-07-05-global-variables.md §4.1.4</p>
 * <p>edu_course.category 字段。</p>
 */
@Getter
@AllArgsConstructor
@Schema(description = "课程类别：1必修 2选修 3公选")
public enum CourseCategoryEnum {

    REQUIRED (1, "必修"),
    OPTIONAL (2, "选修"),
    PUBLIC   (3, "公选");

    @JsonValue
    @Schema(description = "状态码")
    private final int code;

    @Schema(description = "描述")
    private final String label;

    public static CourseCategoryEnum fromCode(int code) {
        for (CourseCategoryEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }
}
