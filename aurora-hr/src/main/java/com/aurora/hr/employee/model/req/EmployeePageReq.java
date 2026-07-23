package com.aurora.hr.employee.model.req;

import com.aurora.common.base.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeePageReq extends PageRequest {

    private Long deptId;
    private Integer status;
    private String keyword;
}
