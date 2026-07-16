package com.aurora.ai.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aurora.ai.agent.entity.AiAgentActionDO;
import com.aurora.ai.agent.mapper.AiAgentActionMapper;
import com.aurora.ai.agent.support.ActionPlanBuilder;
import com.aurora.ai.chat.entity.AiChatMessageDO;
import com.aurora.ai.chat.entity.AiChatSessionDO;
import com.aurora.ai.chat.mapper.AiChatMessageMapper;
import com.aurora.ai.chat.mapper.AiChatSessionMapper;
import com.aurora.ai.chat.model.req.ChatSessionPageReq;
import com.aurora.ai.chat.model.req.CreateSessionReq;
import com.aurora.ai.chat.model.req.SendMessageReq;
import com.aurora.ai.chat.model.resp.ChatMessageResp;
import com.aurora.ai.chat.model.resp.ChatSendResp;
import com.aurora.ai.chat.model.resp.ChatSessionResp;
import com.aurora.ai.chat.service.ChatService;
import com.aurora.ai.chat.support.AgentOrchestrator;
import com.aurora.ai.chat.support.ChatStatus;
import com.aurora.ai.provider.entity.AiModelProviderDO;
import com.aurora.ai.provider.mapper.AiModelProviderMapper;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.aurora.common.response.PageResult;
import com.aurora.common.util.SecurityUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final AiChatSessionMapper sessionMapper;
    private final AiChatMessageMapper messageMapper;
    private final AiAgentActionMapper actionMapper;
    private final AiModelProviderMapper providerMapper;
    private final AgentOrchestrator agentOrchestrator;
    private final ActionPlanBuilder actionPlanBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public ChatSessionResp createSession(CreateSessionReq req) {
        Long userId = SecurityUtil.requireUserId();
        AiModelProviderDO provider = req.getProviderId() == null ? null : requireEnabledProvider(req.getProviderId());
        AiChatSessionDO session = new AiChatSessionDO();
        session.setUserId(userId);
        session.setTitle(StringUtils.hasText(req.getTitle()) ? req.getTitle() : "新对话");
        session.setProviderId(provider == null ? null : provider.getId());
        session.setModel(provider == null ? null : provider.getModel());
        session.setStatus(ChatStatus.ACTIVE);
        session.setLastMessageAt(LocalDateTime.now());
        sessionMapper.insert(session);
        return toSessionResp(session);
    }

    @Override
    public List<ChatSessionResp> listSessions() {
        Long userId = SecurityUtil.requireUserId();
        return sessionMapper.selectList(Wrappers.<AiChatSessionDO>lambdaQuery()
                        .eq(AiChatSessionDO::getUserId, userId)
                        .orderByDesc(AiChatSessionDO::getLastMessageAt)
                        .orderByDesc(AiChatSessionDO::getCreateTime))
                .stream().map(this::toSessionResp).toList();
    }

    @Override
    public PageResult<ChatSessionResp> pageSessions(ChatSessionPageReq req) {
        LambdaQueryWrapper<AiChatSessionDO> wrapper = Wrappers.<AiChatSessionDO>lambdaQuery()
                .eq(req.getUserId() != null, AiChatSessionDO::getUserId, req.getUserId())
                .eq(req.getProviderId() != null, AiChatSessionDO::getProviderId, req.getProviderId())
                .like(StringUtils.hasText(req.getTitle()), AiChatSessionDO::getTitle, req.getTitle())
                .like(StringUtils.hasText(req.getModel()), AiChatSessionDO::getModel, req.getModel())
                .eq(StringUtils.hasText(req.getStatus()), AiChatSessionDO::getStatus, req.getStatus())
                .orderByDesc(AiChatSessionDO::getLastMessageAt)
                .orderByDesc(AiChatSessionDO::getCreateTime);
        Page<AiChatSessionDO> page = sessionMapper.selectPage(new Page<>(req.normalizedPageNum(), req.normalizedPageSize()), wrapper);
        return new PageResult<>(page.getRecords().stream().map(this::toSessionResp).toList(), page.getTotal());
    }

    @Override
    public List<ChatMessageResp> listMessages(Long sessionId) {
        requireOwnSession(sessionId);
        return messageMapper.selectList(Wrappers.<AiChatMessageDO>lambdaQuery()
                        .eq(AiChatMessageDO::getSessionId, sessionId)
                        .orderByAsc(AiChatMessageDO::getCreatedAt)
                        .orderByAsc(AiChatMessageDO::getCreateTime))
                .stream().map(this::toMessageResp).toList();
    }

    @Override
    public List<ChatMessageResp> listSessionMessagesForAdmin(Long sessionId) {
        AiChatSessionDO session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BizException(BizCode.DATA_NOT_FOUND, "会话不存在");
        }
        return messageMapper.selectList(Wrappers.<AiChatMessageDO>lambdaQuery()
                        .eq(AiChatMessageDO::getSessionId, sessionId)
                        .orderByAsc(AiChatMessageDO::getCreatedAt)
                        .orderByAsc(AiChatMessageDO::getCreateTime))
                .stream().map(this::toMessageResp).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatSendResp sendMessage(Long sessionId, SendMessageReq req) {
        Long userId = SecurityUtil.requireUserId();
        AiChatSessionDO session = requireOwnSession(sessionId);
        AiChatMessageDO userMessage = insertMessage(sessionId, "user", req.getContent(), null);
        AgentOrchestrator.AgentDraft draft = agentOrchestrator.draft(sessionId, userId, req.getContent());
        AiAgentActionDO pendingAction = draft.getPendingAction();
        if (pendingAction != null) {
            actionMapper.insert(pendingAction);
        }
        String metadataJson = toJson(Map.of(
                "systemPrompt", draft.getSystemPrompt(),
                "citations", draft.getCitations() == null ? List.of() : draft.getCitations(),
                "toolResult", draft.getToolResult() == null ? Map.of() : draft.getToolResult()
        ));
        AiChatMessageDO assistantMessage = insertMessage(sessionId, "assistant", draft.getAssistantContent(), metadataJson);
        if (pendingAction != null) {
            pendingAction.setMessageId(assistantMessage.getId());
            actionMapper.updateById(pendingAction);
        }
        session.setLastMessageAt(LocalDateTime.now());
        sessionMapper.updateById(session);
        return ChatSendResp.builder()
                .userMessage(toMessageResp(userMessage))
                .assistantMessage(toMessageResp(assistantMessage))
                .citations(draft.getCitations())
                .pendingAction(actionPlanBuilder.toResp(pendingAction))
                .toolResult(draft.getToolResult())
                .build();
    }

    private AiChatSessionDO requireOwnSession(Long sessionId) {
        Long userId = SecurityUtil.requireUserId();
        AiChatSessionDO session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BizException(BizCode.DATA_NOT_FOUND, "会话不存在");
        }
        if (!userId.equals(session.getUserId())) {
            throw new BizException(BizCode.FORBIDDEN, "该会话不属于当前用户");
        }
        return session;
    }

    private AiModelProviderDO requireEnabledProvider(Long providerId) {
        AiModelProviderDO provider = providerMapper.selectById(providerId);
        if (provider == null || provider.getEnabled() == null || provider.getEnabled() != 1) {
            throw new BizException(BizCode.DATA_NOT_FOUND, "Enabled provider does not exist");
        }
        return provider;
    }

    private AiChatMessageDO insertMessage(Long sessionId, String role, String content, String metadataJson) {
        AiChatMessageDO message = new AiChatMessageDO();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setMetadataJson(metadataJson);
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);
        return message;
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private ChatSessionResp toSessionResp(AiChatSessionDO session) {
        return ChatSessionResp.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .title(session.getTitle())
                .providerId(session.getProviderId())
                .model(session.getModel())
                .status(session.getStatus())
                .lastMessageAt(session.getLastMessageAt())
                .createTime(session.getCreateTime())
                .build();
    }

    private ChatMessageResp toMessageResp(AiChatMessageDO message) {
        return ChatMessageResp.builder()
                .id(message.getId())
                .sessionId(message.getSessionId())
                .role(message.getRole())
                .content(message.getContent())
                .metadataJson(message.getMetadataJson())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
