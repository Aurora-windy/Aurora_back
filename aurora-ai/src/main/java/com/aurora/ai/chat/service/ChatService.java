package com.aurora.ai.chat.service;

import com.aurora.ai.chat.model.req.CreateSessionReq;
import com.aurora.ai.chat.model.req.SendMessageReq;
import com.aurora.ai.chat.model.resp.ChatMessageResp;
import com.aurora.ai.chat.model.resp.ChatSendResp;
import com.aurora.ai.chat.model.resp.ChatSessionResp;

import java.util.List;

public interface ChatService {
    ChatSessionResp createSession(CreateSessionReq req);

    List<ChatSessionResp> listSessions();

    List<ChatMessageResp> listMessages(Long sessionId);

    ChatSendResp sendMessage(Long sessionId, SendMessageReq req);
}