package com.aurora.hr.employee.model.resp;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class EmployeeResp {

    private Long id;
    private String empNo;
    private Long userId;
    private Long deptId;
    private Long positionId;
    private String name;
    private Integer gender;
    private String phone;
    private LocalDate entryDate;
    private Integer status;
    private LocalDateTime createTime;
}
