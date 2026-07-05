package com.aurora.edu.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 教师职称枚举
 *
 * <p>spec: docs/specs/2026-07-05-global-variables.md §4.1.4</p>
 * <p>edu_teacher.title 字段。</p>
 */
@Getter
@AllArgsConstructor
@Schema(description = "教师职称：1教授 2副教授 3讲师 4助教")
public enum TeacherTitleEnum {

    PROFESSOR       (1, "教授"),
    ASSOCIATE_PROF  (2, "副教授"),
    LECTURER        (3, "讲师"),
    ASSISTANT       (4, "助教");

    @JsonValue
    @Schema(description = "状态码")
    private final int code;

    @Schema(description = "描述")
    private final String label;

    public static TeacherTitleEnum fromCode(int code) {
        for (TeacherTitleEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }
}
