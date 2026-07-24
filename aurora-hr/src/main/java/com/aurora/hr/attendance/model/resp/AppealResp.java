package com.aurora.hr.attendance.model.resp;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 申诉记录响应。
 */
@Data
@Builder
public class AppealResp {

    private Long id;
    private Long attendanceId;
    private Long empId;
    private String empName;
    private String empNo;
    private LocalDate attendanceDate;
    private Integer attendanceStatus;
    private String reason;
    private Integer status;
    private Long auditUserId;
    private String auditRemark;
    private LocalDateTime auditTime;
    private LocalDateTime createTime;
}
