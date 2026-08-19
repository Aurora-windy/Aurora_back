package com.aurora.ai.chat.service;

import com.aurora.ai.chat.model.req.CreateSessionReq;
import com.aurora.ai.chat.model.req.ChatSessionPageReq;
import com.aurora.ai.chat.model.req.SendMessageReq;
import com.aurora.ai.chat.model.resp.ChatMessageResp;
import com.aurora.ai.chat.model.resp.ChatSendResp;
import com.aurora.ai.chat.model.resp.ChatSessionResp;
import com.aurora.common.response.PageResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface ChatService {
    ChatSessionResp createSession(CreateSessionReq req);

    List<ChatSessionResp> listSessions();

    PageResult<ChatSessionResp> pageSessions(ChatSessionPageReq req);

    List<ChatMessageResp> listMessages(Long sessionId);

    List<ChatMessageResp> listSessionMessagesForAdmin(Long sessionId);

    ChatSendResp sendMessage(Long sessionId, SendMessageReq req);

    void streamMessage(Long sessionId, SendMessageReq req, SseEmitter emitter);
}
