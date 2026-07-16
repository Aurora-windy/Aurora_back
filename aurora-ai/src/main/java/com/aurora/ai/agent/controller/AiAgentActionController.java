package com.aurora.ai.agent.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.ai.agent.model.resp.ActionResultResp;
import com.aurora.ai.agent.service.AgentActionService;
import com.aurora.common.constant.PermCodeConst;
import com.aurora.common.response.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/actions")
public class AiAgentActionController {

    private final AgentActionService agentActionService;

    @SaCheckPermission(PermCodeConst.Ai.Chat.USE)
    @PostMapping("/{actionId}/confirm")
    public Result<ActionResultResp> confirm(@PathVariable Long actionId) {
        return Result.ok(agentActionService.confirm(actionId));
    }

    @SaCheckPermission(PermCodeConst.Ai.Chat.USE)
    @PostMapping("/{actionId}/reject")
    public Result<ActionResultResp> reject(@PathVariable Long actionId) {
        return Result.ok(agentActionService.reject(actionId));
    }
}