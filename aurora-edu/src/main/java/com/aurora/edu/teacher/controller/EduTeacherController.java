package com.aurora.edu.teacher.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.common.constant.PermCodeConst;
import com.aurora.common.response.PageResult;
import com.aurora.common.response.Result;
import com.aurora.edu.teacher.model.req.TeacherAddReq;
import com.aurora.edu.teacher.model.req.TeacherPageReq;
import com.aurora.edu.teacher.model.req.TeacherUpdateReq;
import com.aurora.edu.teacher.model.resp.TeacherResp;
import com.aurora.edu.teacher.service.EduTeacherService;
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
@RequestMapping("/edu/teachers")
public class EduTeacherController {
    private final EduTeacherService teacherService;

    @SaCheckPermission(PermCodeConst.Edu.Teacher.LIST)
    @GetMapping
    public Result<PageResult<TeacherResp>> page(TeacherPageReq req) { return Result.ok(teacherService.page(req)); }

    @SaCheckPermission(PermCodeConst.Edu.Teacher.LIST)
    @GetMapping("/{id}")
    public Result<TeacherResp> detail(@PathVariable Long id) { return Result.ok(teacherService.detail(id)); }

    @SaCheckPermission(PermCodeConst.Edu.Teacher.ADD)
    @PostMapping
    public Result<Long> add(@RequestBody @Valid TeacherAddReq req) { return Result.ok(teacherService.add(req)); }

    @SaCheckPermission(PermCodeConst.Edu.Teacher.EDIT)
    @PutMapping
    public Result<Boolean> update(@RequestBody @Valid TeacherUpdateReq req) { teacherService.update(req); return Result.ok(Boolean.TRUE); }

    @SaCheckPermission(PermCodeConst.Edu.Teacher.REMOVE)
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) { teacherService.delete(id); return Result.ok(Boolean.TRUE); }
}
