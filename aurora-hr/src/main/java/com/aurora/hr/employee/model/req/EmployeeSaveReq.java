package com.aurora.hr.employee.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class EmployeeSaveReq {

    private Long id;
    @NotBlank(message = "工号不能为空")
    private String empNo;
    private Long userId;
    @NotNull(message = "所属部门不能为空")
    private Long deptId;
    @NotNull(message = "所属岗位不能为空")
    private Long positionId;
    @NotBlank(message = "员工姓名不能为空")
    private String name;
    private Integer gender;
    private String phone;
    private LocalDate entryDate;
    private Integer status;
}
