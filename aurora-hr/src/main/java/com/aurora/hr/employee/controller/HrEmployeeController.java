package com.aurora.hr.employee.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.common.constant.PermCodeConst;
import com.aurora.common.response.PageResult;
import com.aurora.common.response.Result;
import com.aurora.hr.employee.model.req.EmployeePageReq;
import com.aurora.hr.employee.model.req.EmployeeSaveReq;
import com.aurora.hr.employee.model.resp.EmployeeResp;
import com.aurora.hr.employee.service.HrEmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/hr/employees")
public class HrEmployeeController {

    private final HrEmployeeService employeeService;

    @SaCheckPermission(PermCodeConst.Hr.Employee.LIST)
    @GetMapping
    public Result<PageResult<EmployeeResp>> page(EmployeePageReq req) {
        return Result.ok(employeeService.page(req));
    }

    @SaCheckPermission(PermCodeConst.Hr.Employee.LIST)
    @GetMapping("/{id}")
    public Result<EmployeeResp> detail(@PathVariable Long id) {
        return Result.ok(employeeService.detail(id));
    }

    @SaCheckPermission(PermCodeConst.Hr.Employee.ADD)
    @PostMapping
    public Result<Long> add(@RequestBody @Valid EmployeeSaveReq req) {
        return Result.ok(employeeService.add(req));
    }

    @SaCheckPermission(PermCodeConst.Hr.Employee.EDIT)
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody @Valid EmployeeSaveReq req) {
        employeeService.update(id, req);
        return Result.ok(Boolean.TRUE);
    }

    @SaCheckPermission(PermCodeConst.Hr.Employee.REMOVE)
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return Result.ok(Boolean.TRUE);
    }
}
