package com.aurora.ai.chat.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.ai.chat.model.req.CreateSessionReq;
import com.aurora.ai.chat.model.req.SendMessageReq;
import com.aurora.ai.chat.model.resp.ChatMessageResp;
import com.aurora.ai.chat.model.resp.ChatSendResp;
import com.aurora.ai.chat.model.resp.ChatSessionResp;
import com.aurora.ai.chat.service.ChatService;
import com.aurora.common.constant.PermCodeConst;
import com.aurora.common.response.Result;
import jakarta.validation.Valid;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/sessions")
public class AiChatController {

    private final ChatService chatService;

    @SaCheckPermission(PermCodeConst.Ai.Chat.USE)
    @PostMapping
    public Result<ChatSessionResp> createSession(@RequestBody CreateSessionReq req) {
        return Result.ok(chatService.createSession(req));
    }

    @SaCheckPermission(PermCodeConst.Ai.Chat.USE)
    @GetMapping
    public Result<List<ChatSessionResp>> listSessions() {
        return Result.ok(chatService.listSessions());
    }

    @SaCheckPermission(PermCodeConst.Ai.Chat.USE)
    @GetMapping("/{sessionId}/messages")
    public Result<List<ChatMessageResp>> listMessages(@PathVariable Long sessionId) {
        return Result.ok(chatService.listMessages(sessionId));
    }

    @SaCheckPermission(PermCodeConst.Ai.Chat.USE)
    @PostMapping("/{sessionId}/messages")
    public Result<ChatSendResp> sendMessage(@PathVariable Long sessionId, @RequestBody @Valid SendMessageReq req) {
        return Result.ok(chatService.sendMessage(sessionId, req));
    }

    @SaCheckPermission(PermCodeConst.Ai.Chat.USE)
    @PostMapping("/{sessionId}/messages/stream")
    public SseEmitter streamMessage(@PathVariable Long sessionId, @RequestBody @Valid SendMessageReq req) {
        SseEmitter emitter = new SseEmitter(300_000L);
        chatService.streamMessage(sessionId, req, emitter);
        return emitter;
    }
}
