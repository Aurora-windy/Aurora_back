package com.aurora.hr.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 考勤状态枚举
 *
 * <p>spec: docs/specs/2026-07-05-global-variables.md §4.1.3</p>
 * <p>hr_attendance.status 字段。0=缺卡由定时任务生成，1/2/3 由签到时间自动判定，4 由申诉通过后覆盖。</p>
 */
@Getter
@AllArgsConstructor
@Schema(description = "考勤状态：0缺卡 1正常 2迟到 3早退 4正常(已审批)")
public enum AttendanceStatusEnum {

    MISSING     (0, "缺卡"),
    NORMAL      (1, "正常"),
    LATE        (2, "迟到"),
    EARLY_LEAVE (3, "早退"),
    APPROVED    (4, "正常(已审批)");

    @JsonValue
    @Schema(description = "状态码")
    private final int code;

    @Schema(description = "描述")
    private final String label;

    public static AttendanceStatusEnum fromCode(int code) {
        for (AttendanceStatusEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }
}
