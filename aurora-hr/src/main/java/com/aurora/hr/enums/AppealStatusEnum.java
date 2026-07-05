package com.aurora.hr.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 考勤申诉状态枚举
 *
 * <p>spec: docs/specs/2026-07-05-global-variables.md §4.1.3</p>
 * <p>hr_attendance_appeal.status 字段。</p>
 */
@Getter
@AllArgsConstructor
@Schema(description = "申诉状态：0待审核 1通过 2驳回")
public enum AppealStatusEnum {

    PENDING  (0, "待审核"),
    APPROVED (1, "通过"),
    REJECTED (2, "驳回");

    @JsonValue
    @Schema(description = "状态码")
    private final int code;

    @Schema(description = "描述")
    private final String label;

    public static AppealStatusEnum fromCode(int code) {
        for (AppealStatusEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }
}
