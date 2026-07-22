package com.aurora.oj.submission.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.common.constant.PermCodeConst;
import com.aurora.common.response.PageResult;
import com.aurora.common.response.Result;
import com.aurora.oj.submission.model.req.SubmissionCreateReq;
import com.aurora.oj.submission.model.req.SubmissionPageReq;
import com.aurora.oj.submission.model.resp.SubmissionResp;
import com.aurora.oj.submission.service.OjSubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/oj/submissions")
public class OjSubmissionController {

    private final OjSubmissionService submissionService;

    @SaCheckPermission(PermCodeConst.Oj.SUBMISSION_SUBMIT)
    @PostMapping
    public Result<Long> submit(@RequestBody @Valid SubmissionCreateReq req) {
        return Result.ok(submissionService.submit(req));
    }

    @SaCheckPermission(PermCodeConst.Oj.SUBMISSION_VIEW_MY)
    @GetMapping("/my")
    public Result<PageResult<SubmissionResp>> mySubmissions(SubmissionPageReq req) {
        return Result.ok(submissionService.mySubmissions(req));
    }

    @SaCheckPermission(PermCodeConst.Oj.Problem.LIST)
    @GetMapping
    public Result<PageResult<SubmissionResp>> page(SubmissionPageReq req) {
        return Result.ok(submissionService.page(req));
    }
}
