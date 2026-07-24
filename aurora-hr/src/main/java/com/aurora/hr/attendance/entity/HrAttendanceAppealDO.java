package com.aurora.hr.attendance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aurora.common.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 考勤申诉实体。
 *
 * <p>spec: docs/specs/2026-07-24-hr-attendance.md</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_attendance_appeal")
public class HrAttendanceAppealDO extends BaseDO {

    private Long attendanceId;
    private Long empId;
    private String reason;
    private Integer status;
    private Long auditUserId;
    private String auditRemark;
    private LocalDateTime auditTime;
}
