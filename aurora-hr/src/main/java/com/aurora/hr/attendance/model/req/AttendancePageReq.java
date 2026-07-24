package com.aurora.hr.attendance.model.req;

import com.aurora.common.base.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 考勤记录分页查询请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AttendancePageReq extends PageRequest {

    private Long deptId;
    private Long empId;
    private Integer status;
    private LocalDate dateFrom;
    private LocalDate dateTo;
}
