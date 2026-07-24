package com.aurora.hr.attendance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aurora.common.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考勤记录实体。
 *
 * <p>spec: docs/specs/2026-07-24-hr-attendance.md</p>
 * <p>每员工每天一条记录，含上班打卡和下班打卡时间。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_attendance")
public class HrAttendanceDO extends BaseDO {

    private Long empId;
    private LocalDate attendanceDate;
    private LocalDateTime clockInTime;
    private LocalDateTime clockOutTime;
    private Integer status;
    private String remark;
}
