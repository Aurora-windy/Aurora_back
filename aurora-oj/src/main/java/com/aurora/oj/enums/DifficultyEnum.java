package com.aurora.oj.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 题目难度枚举
 *
 * <p>spec: docs/specs/2026-07-05-global-variables.md §4.1.5</p>
 * <p>oj_problem.difficulty 字段。</p>
 */
@Getter
@AllArgsConstructor
@Schema(description = "题目难度：1简单 2中等 3困难")
public enum DifficultyEnum {

    EASY   (1, "简单"),
    MEDIUM (2, "中等"),
    HARD   (3, "困难");

    @JsonValue
    @Schema(description = "状态码")
    private final int code;

    @Schema(description = "描述")
    private final String label;

    public static DifficultyEnum fromCode(int code) {
        for (DifficultyEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }
}
