package com.aurora.edu.selection.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.common.constant.PermCodeConst;
import com.aurora.common.response.Result;
import com.aurora.edu.selection.model.req.SelectionReq;
import com.aurora.edu.selection.model.resp.SelectionResp;
import com.aurora.edu.selection.service.EduSelectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/edu/selections")
public class EduSelectionController {
    private final EduSelectionService selectionService;

    @GetMapping("/my")
    public Result<List<SelectionResp>> mySelections() { return Result.ok(selectionService.mySelections()); }

    @SaCheckPermission(PermCodeConst.Edu.SELECTION_SELECT)
    @PostMapping
    public Result<Long> selectCourse(@RequestBody @Valid SelectionReq req) { return Result.ok(selectionService.selectCourse(req.getCourseId())); }

    @SaCheckPermission(PermCodeConst.Edu.SELECTION_DROP)
    @DeleteMapping("/{courseId}")
    public Result<Boolean> dropCourse(@PathVariable Long courseId) { selectionService.dropCourse(courseId); return Result.ok(Boolean.TRUE); }
}
