package com.aurora.oj.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 判题结果状态枚举
 *
 * <p>spec: docs/specs/2026-07-05-global-variables.md §4.1.5</p>
 * <p>oj_submission.status 字段。0=PENDING 由提交瞬间写入；1-7 由沙箱回填。</p>
 */
@Getter
@AllArgsConstructor
@Schema(description = "判题结果：0排队 1AC 2WA 3TLE 4MLE 5RE 6CE 7SE")
public enum JudgeStatusEnum {

    PENDING                (0, "Pending",              "排队中"),
    ACCEPTED               (1, "Accepted",             "答案正确"),
    WRONG_ANSWER           (2, "Wrong Answer",         "答案错误"),
    TIME_LIMIT_EXCEEDED    (3, "Time Limit Exceeded",  "时间超限"),
    MEMORY_LIMIT_EXCEEDED  (4, "Memory Limit Exceeded","内存超限"),
    RUNTIME_ERROR          (5, "Runtime Error",        "运行错误"),
    COMPILE_ERROR          (6, "Compile Error",        "编译错误"),
    SYSTEM_ERROR           (7, "System Error",         "系统错误");

    @JsonValue
    @Schema(description = "状态码")
    private final int code;

    @Schema(description = "英文缩写（OJ 题目通用）")
    private final String abbr;

    @Schema(description = "中文描述")
    private final String label;

    public static JudgeStatusEnum fromCode(int code) {
        for (JudgeStatusEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }
}
