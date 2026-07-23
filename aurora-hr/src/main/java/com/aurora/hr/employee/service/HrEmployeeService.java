package com.aurora.hr.employee.service;

import com.aurora.common.response.PageResult;
import com.aurora.hr.employee.model.req.EmployeePageReq;
import com.aurora.hr.employee.model.req.EmployeeSaveReq;
import com.aurora.hr.employee.model.resp.EmployeeResp;

public interface HrEmployeeService {

    PageResult<EmployeeResp> page(EmployeePageReq req);

    EmployeeResp detail(Long id);

    Long add(EmployeeSaveReq req);

    void update(Long id, EmployeeSaveReq req);

    void delete(Long id);
}
