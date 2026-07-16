package com.aurora.ai.chat.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.ai.chat.model.req.ChatSessionPageReq;
import com.aurora.ai.chat.model.resp.ChatMessageResp;
import com.aurora.ai.chat.model.resp.ChatSessionResp;
import com.aurora.ai.chat.service.ChatService;
import com.aurora.common.constant.PermCodeConst;
import com.aurora.common.response.PageResult;
import com.aurora.common.response.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/admin/sessions")
public class AiChatSessionAdminController {

    private final ChatService chatService;

    @SaCheckPermission(PermCodeConst.Ai.Session.LIST)
    @GetMapping
    public Result<PageResult<ChatSessionResp>> page(ChatSessionPageReq req) {
        return Result.ok(chatService.pageSessions(req));
    }

    @SaCheckPermission(PermCodeConst.Ai.Session.LIST)
    @GetMapping("/{sessionId}/messages")
    public Result<List<ChatMessageResp>> listMessages(@PathVariable Long sessionId) {
        return Result.ok(chatService.listSessionMessagesForAdmin(sessionId));
    }
}
