package com.aurora.oj.problem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.common.constant.PermCodeConst;
import com.aurora.common.response.PageResult;
import com.aurora.common.response.Result;
import com.aurora.oj.problem.model.req.ProblemPageReq;
import com.aurora.oj.problem.model.req.ProblemSaveReq;
import com.aurora.oj.problem.model.resp.ProblemResp;
import com.aurora.oj.problem.service.OjProblemService;
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
@RequestMapping("/oj/problems")
public class OjProblemController {

    private final OjProblemService problemService;

    @SaCheckPermission(PermCodeConst.Oj.Problem.LIST)
    @GetMapping
    public Result<PageResult<ProblemResp>> page(ProblemPageReq req) {
        return Result.ok(problemService.page(req));
    }

    @SaCheckPermission(PermCodeConst.Oj.Problem.LIST)
    @GetMapping("/available")
    public Result<PageResult<ProblemResp>> available(ProblemPageReq req) {
        return Result.ok(problemService.available(req));
    }

    @SaCheckPermission(PermCodeConst.Oj.Problem.DETAIL)
    @GetMapping("/available/{id}")
    public Result<ProblemResp> availableDetail(@PathVariable Long id) {
        return Result.ok(problemService.availableDetail(id));
    }

    @SaCheckPermission(PermCodeConst.Oj.Problem.DETAIL)
    @GetMapping("/{id}")
    public Result<ProblemResp> detail(@PathVariable Long id) {
        return Result.ok(problemService.detail(id));
    }

    @SaCheckPermission(PermCodeConst.Oj.Problem.ADD)
    @PostMapping
    public Result<Long> add(@RequestBody @Valid ProblemSaveReq req) {
        return Result.ok(problemService.add(req));
    }

    @SaCheckPermission(PermCodeConst.Oj.Problem.EDIT)
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody @Valid ProblemSaveReq req) {
        problemService.update(id, req);
        return Result.ok(Boolean.TRUE);
    }

    @SaCheckPermission(PermCodeConst.Oj.Problem.REMOVE)
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        problemService.delete(id);
        return Result.ok(Boolean.TRUE);
    }
}
