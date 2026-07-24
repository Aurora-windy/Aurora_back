package com.aurora.hr.attendance.model.resp;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考勤记录响应。
 */
@Data
@Builder
public class AttendanceResp {

    private Long id;
    private Long empId;
    private String empName;
    private String empNo;
    private String deptName;
    private LocalDate attendanceDate;
    private LocalDateTime clockInTime;
    private LocalDateTime clockOutTime;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
}
