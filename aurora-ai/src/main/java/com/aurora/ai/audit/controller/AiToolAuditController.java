package com.aurora.ai.audit.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.ai.audit.model.req.AiToolCallLogPageReq;
import com.aurora.ai.audit.model.resp.AiToolCallLogResp;
import com.aurora.ai.audit.service.AiToolAuditService;
import com.aurora.common.constant.PermCodeConst;
import com.aurora.common.response.PageResult;
import com.aurora.common.response.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/admin/audit")
public class AiToolAuditController {

    private final AiToolAuditService auditService;

    @SaCheckPermission(PermCodeConst.Ai.Audit.LIST)
    @GetMapping("/tool-calls")
    public Result<PageResult<AiToolCallLogResp>> page(AiToolCallLogPageReq req) {
        return Result.ok(auditService.page(req));
    }
}
