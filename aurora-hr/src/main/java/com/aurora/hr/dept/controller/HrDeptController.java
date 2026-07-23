package com.aurora.hr.dept.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.common.constant.PermCodeConst;
import com.aurora.common.response.Result;
import com.aurora.hr.dept.model.req.DeptSaveReq;
import com.aurora.hr.dept.model.resp.DeptResp;
import com.aurora.hr.dept.service.HrDeptService;
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

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/hr/depts")
public class HrDeptController {

    private final HrDeptService deptService;

    @SaCheckPermission(PermCodeConst.Hr.Dept.LIST)
    @GetMapping
    public Result<List<DeptResp>> list() {
        return Result.ok(deptService.list());
    }

    @SaCheckPermission(PermCodeConst.Hr.Dept.ADD)
    @PostMapping
    public Result<Long> add(@RequestBody @Valid DeptSaveReq req) {
        return Result.ok(deptService.add(req));
    }

    @SaCheckPermission(PermCodeConst.Hr.Dept.EDIT)
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody @Valid DeptSaveReq req) {
        deptService.update(id, req);
        return Result.ok(Boolean.TRUE);
    }

    @SaCheckPermission(PermCodeConst.Hr.Dept.REMOVE)
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        deptService.delete(id);
        return Result.ok(Boolean.TRUE);
    }
}
