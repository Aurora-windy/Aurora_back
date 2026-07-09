package com.aurora.edu.course.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.common.constant.PermCodeConst;
import com.aurora.common.response.PageResult;
import com.aurora.common.response.Result;
import com.aurora.edu.course.model.req.CourseAddReq;
import com.aurora.edu.course.model.req.CoursePageReq;
import com.aurora.edu.course.model.req.CourseUpdateReq;
import com.aurora.edu.course.model.resp.CourseResp;
import com.aurora.edu.course.service.EduCourseService;
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
@RequestMapping("/edu/courses")
public class EduCourseController {
    private final EduCourseService courseService;

    @SaCheckPermission(PermCodeConst.Edu.Course.LIST)
    @GetMapping
    public Result<PageResult<CourseResp>> page(CoursePageReq req) { return Result.ok(courseService.page(req)); }

    @SaCheckPermission(PermCodeConst.Edu.Course.LIST)
    @GetMapping("/available")
    public Result<PageResult<CourseResp>> available(CoursePageReq req) { return Result.ok(courseService.available(req)); }

    @SaCheckPermission(PermCodeConst.Edu.Course.LIST)
    @GetMapping("/{id}")
    public Result<CourseResp> detail(@PathVariable Long id) { return Result.ok(courseService.detail(id)); }

    @SaCheckPermission(PermCodeConst.Edu.Course.ADD)
    @PostMapping
    public Result<Long> add(@RequestBody @Valid CourseAddReq req) { return Result.ok(courseService.add(req)); }

    @SaCheckPermission(PermCodeConst.Edu.Course.EDIT)
    @PutMapping
    public Result<Boolean> update(@RequestBody @Valid CourseUpdateReq req) { courseService.update(req); return Result.ok(Boolean.TRUE); }

    @SaCheckPermission(PermCodeConst.Edu.Course.REMOVE)
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) { courseService.delete(id); return Result.ok(Boolean.TRUE); }
}
