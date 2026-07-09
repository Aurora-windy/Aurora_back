package com.aurora.edu.student.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.common.constant.PermCodeConst;
import com.aurora.common.response.PageResult;
import com.aurora.common.response.Result;
import com.aurora.edu.student.model.req.StudentAddReq;
import com.aurora.edu.student.model.req.StudentPageReq;
import com.aurora.edu.student.model.req.StudentUpdateReq;
import com.aurora.edu.student.model.resp.StudentResp;
import com.aurora.edu.student.service.EduStudentService;
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
@RequestMapping("/edu/students")
public class EduStudentController {
    private final EduStudentService studentService;

    @SaCheckPermission(PermCodeConst.Edu.Student.LIST)
    @GetMapping
    public Result<PageResult<StudentResp>> page(StudentPageReq req) { return Result.ok(studentService.page(req)); }

    @SaCheckPermission(PermCodeConst.Edu.Student.LIST)
    @GetMapping("/{id}")
    public Result<StudentResp> detail(@PathVariable Long id) { return Result.ok(studentService.detail(id)); }

    @SaCheckPermission(PermCodeConst.Edu.Student.ADD)
    @PostMapping
    public Result<Long> add(@RequestBody @Valid StudentAddReq req) { return Result.ok(studentService.add(req)); }

    @SaCheckPermission(PermCodeConst.Edu.Student.EDIT)
    @PutMapping
    public Result<Boolean> update(@RequestBody @Valid StudentUpdateReq req) { studentService.update(req); return Result.ok(Boolean.TRUE); }

    @SaCheckPermission(PermCodeConst.Edu.Student.REMOVE)
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) { studentService.delete(id); return Result.ok(Boolean.TRUE); }
}
