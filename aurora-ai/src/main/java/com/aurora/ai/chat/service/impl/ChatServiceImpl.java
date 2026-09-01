package com.aurora.ai.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aurora.ai.agent.entity.AiAgentActionDO;
import com.aurora.ai.agent.mapper.AiAgentActionMapper;
import com.aurora.ai.agent.support.ActionPlanBuilder;
import com.aurora.ai.agent.support.AgentActionStatus;
import com.aurora.ai.tool.model.AiToolResult;
import com.aurora.ai.chat.entity.AiChatMessageDO;
import com.aurora.ai.chat.entity.AiChatSessionDO;
import com.aurora.ai.chat.mapper.AiChatMessageMapper;
import com.aurora.ai.chat.mapper.AiChatSessionMapper;
import com.aurora.ai.chat.model.req.ChatSessionPageReq;
import com.aurora.ai.chat.model.req.CreateSessionReq;
import com.aurora.ai.chat.model.req.SendMessageReq;
import com.aurora.ai.chat.model.req.LocalFilesEnabledReq;
import com.aurora.ai.chat.model.resp.ChatMessageResp;
import com.aurora.ai.chat.model.resp.ChatSendResp;
import com.aurora.ai.agent.model.resp.ActionResp;
import com.aurora.ai.chat.model.resp.ChatSessionResp;
import com.aurora.ai.knowledge.model.resp.KnowledgeCitation;
import com.aurora.ai.chat.service.ChatService;
import com.aurora.ai.chat.support.AgentOrchestrator;
import com.aurora.ai.chat.support.ChatStatus;
import com.aurora.ai.chat.support.PromptBuilder;
import com.aurora.ai.provider.entity.AiModelProviderDO;
import com.aurora.ai.provider.mapper.AiModelProviderMapper;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.aurora.common.response.PageResult;
import com.aurora.common.util.SecurityUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final String USAGE_CHAT = "CHAT";
    private static final String USAGE_BOTH = "BOTH";
    private final AiChatSessionMapper sessionMapper;
    private final AiChatMessageMapper messageMapper;
    private final AiAgentActionMapper actionMapper;
    private final AiModelProviderMapper providerMapper;
    private final AgentOrchestrator agentOrchestrator;
    private final ActionPlanBuilder actionPlanBuilder;
    private final PromptBuilder promptBuilder;
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
    @Transactional(rollbackFor = Exception.class)
    public void setLocalFilesEnabled(Long sessionId, LocalFilesEnabledReq req) {
        AiChatSessionDO session = requireOwnSession(sessionId);
        session.setLocalFilesEnabled(Boolean.TRUE.equals(req.getEnabled()) ? 1 : 0);
        sessionMapper.updateById(session);
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
        AgentOrchestrator.AgentDraft draft = agentOrchestrator.draft(sessionId, userId, req.getContent(), req.getUseKnowledgeBase());
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

    @Override
    public void streamMessage(Long sessionId, SendMessageReq req, SseEmitter emitter) {
        emitter.onTimeout(() -> emitter.complete());
        Long userId = SecurityUtil.requireUserId();
        AiChatSessionDO session = requireOwnSession(sessionId);

        AiChatMessageDO userMessage = insertMessage(sessionId, "user", req.getContent(), null);
        AgentOrchestrator.StreamContext ctx = agentOrchestrator.prepare(sessionId, userId, req.getContent(), req.getUseKnowledgeBase());

        // FAST_MUTATION：正则确定性命中修改意图，直接挂起待确认，不走 LLM
        if (ctx.getPendingAction() != null) {
            log.info("agent.path=FAST_MUTATION sessionId={} tool={}", sessionId, ctx.getPendingAction().getToolName());
            actionMapper.insert(ctx.getPendingAction());
            try {
                emitter.send(SseEmitter.event().name("pending").data(actionPlanBuilder.toResp(ctx.getPendingAction())));
            } catch (IOException ignored) {
            }
            finishStream(emitter, session, ctx,
                    "已识别到 EDU 数据修改请求，并生成待确认操作计划。确认前不会变更任何 EDU 数据。", null, null);
            return;
        }

        // FAST_QUERY / FC 循环在异步线程内进行。工具执行要过 Sa-Token RBAC（StpUtil 读请求上下文），
        // 需把 RequestAttributes 显式带进异步线程（SSE 期间请求对象仍有效）
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            StringBuilder seen = new StringBuilder();
            try {
                AgentOrchestrator.AgentRunResult result = agentOrchestrator.streamConversation(
                        sessionId, userId, req.getContent(), ctx, sseListener(emitter, seen));

                if (result.getPendingAction() != null) {
                    // FC 挂起：插入待确认动作并推送 pending 事件（确认流复用正则路径的同一套 action）
                    AiAgentActionDO action = result.getPendingAction();
                    actionMapper.insert(action);
                    try {
                        emitter.send(SseEmitter.event().name("pending").data(actionPlanBuilder.toResp(action)));
                    } catch (IOException ignored) {
                    }
                }
                long usage = result.getTotalTokens() > 0
                        ? result.getTotalTokens()
                        : Math.max(1L, result.getContent().length() / 2); // 上游未回传用量时按字符数估算
                finishStream(emitter, session, ctx, result.getContent(), usage, result.getToolTrace());
            } catch (Exception e) {
                // 中途断流：已发出的 token 不能丢，追加中断标记后落库
                String msg = seen.length() > 0
                        ? seen + "\n\n（流式生成中断：" + e.getMessage() + "）"
                        : "模型调用失败：" + e.getMessage();
                finishStream(emitter, session, ctx, msg, 0L, null);
            } finally {
                RequestContextHolder.resetRequestAttributes();
            }
        });
    }

    private void finishStream(SseEmitter emitter, AiChatSessionDO session, AgentOrchestrator.StreamContext ctx,
                              String content, Long usage, List<Map<String, Object>> toolTrace) {
        List<KnowledgeCitation> citations = ctx.getCitations() == null ? List.of() : ctx.getCitations();
        AiChatMessageDO assistantMessage = insertMessage(session.getId(), "assistant", content,
                toJson(Map.of("systemPrompt", ctx.getSystemPrompt(),
                        "citations", citations,
                        "toolTrace", toolTrace == null ? List.of() : toolTrace)));
        if (ctx.getPendingAction() != null) {
            ctx.getPendingAction().setMessageId(assistantMessage.getId());
            actionMapper.updateById(ctx.getPendingAction());
        }
        session.setLastMessageAt(LocalDateTime.now());
        sessionMapper.updateById(session);
        sendDone(emitter, assistantMessage.getId(), citations, usage == null ? 0L : usage);
    }

    private void sendDone(SseEmitter emitter, Long messageId, List<?> citations, long totalTokens) {
        sendDone(emitter, messageId, citations, totalTokens, null);
    }

    /** done 事件可选携带 resumedFromActionId（T1 §7.1），前端据此区分续聊产出的消息 */
    private void sendDone(SseEmitter emitter, Long messageId, List<?> citations, long totalTokens, Long resumedFromActionId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messageId", messageId);
        payload.put("citations", citations);
        payload.put("totalTokens", totalTokens);
        if (resumedFromActionId != null) {
            payload.put("resumedFromActionId", resumedFromActionId);
        }
        try {
            emitter.send(SseEmitter.event().name("done").data(payload));
        } catch (IOException ignored) {
        }
        emitter.complete();
    }

    // ==================== 确认流续聊（T5，T1 §7.2） ====================

    @Override
    public void resumeStream(Long sessionId, Long actionId, SseEmitter emitter) {
        emitter.onTimeout(() -> emitter.complete());
        Long userId = SecurityUtil.requireUserId();
        AiChatSessionDO session = requireOwnSession(sessionId);
        AiAgentActionDO action = requireResumableAction(sessionId, actionId, userId);

        // 防重复续聊：同 action 已 resume 过则幂等返回既有消息（前端重试/重复触发场景）
        AiChatMessageDO existing = findResumedMessage(sessionId, actionId);
        if (existing != null) {
            sendDone(emitter, existing.getId(), List.of(), 0L, actionId);
            return;
        }

        // 重建上下文：原 user 消息 = 计划 assistant 消息之前最近一条；systemPrompt 优先复用计划消息 metadata。
        // 三元单次赋值保证 systemPrompt 事实最终（下方异步 lambda 要捕获）
        AiChatMessageDO originalUser = findOriginalUserMessage(sessionId, action.getMessageId());
        String storedPrompt = extractStoredSystemPrompt(action.getMessageId());
        String systemPrompt = StringUtils.hasText(storedPrompt)
                ? storedPrompt
                : promptBuilder.buildSystemPrompt(List.of());
        Set<Long> excludeIds = new HashSet<>();
        if (originalUser != null) {
            excludeIds.add(originalUser.getId());
        }
        if (action.getMessageId() != null) {
            excludeIds.add(action.getMessageId());
        }
        String originalContent = originalUser != null && StringUtils.hasText(originalUser.getContent())
                ? originalUser.getContent()
                : extractRawUserRequest(action.getParamsJson());
        AgentOrchestrator.ResumeContext resumeCtx =
                new AgentOrchestrator.ResumeContext(systemPrompt, originalContent, excludeIds);

        // 与 streamMessage 相同：工具执行需 Sa-Token 上下文，显式传进异步线程
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            StringBuilder seen = new StringBuilder();
            try {
                AgentOrchestrator.AgentRunResult result =
                        agentOrchestrator.streamResume(sessionId, userId, action, resumeCtx, sseListener(emitter, seen));
                long usage = result.getTotalTokens() > 0
                        ? result.getTotalTokens()
                        : Math.max(1L, result.getContent().length() / 2);
                finishResumeStream(emitter, session, actionId, systemPrompt, result.getContent(), usage, result.getToolTrace());
            } catch (Exception e) {
                String msg = seen.length() > 0
                        ? seen + "\n\n（续聊生成中断：" + e.getMessage() + "）"
                        : "续聊失败：" + e.getMessage();
                finishResumeStream(emitter, session, actionId, systemPrompt, msg, 0L, null);
            } finally {
                RequestContextHolder.resetRequestAttributes();
            }
        });
    }

    /** 续聊产出的 assistant 消息落库（metadata 带 resumedFromActionId，LinkedHashMap 保证其为末键，供防重复 LIKE 匹配） */
    private void finishResumeStream(SseEmitter emitter, AiChatSessionDO session, Long actionId, String systemPrompt,
                                    String content, Long usage, List<Map<String, Object>> toolTrace) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("systemPrompt", systemPrompt);
        metadata.put("citations", List.of());
        metadata.put("toolTrace", toolTrace == null ? List.of() : toolTrace);
        metadata.put("resumedFromActionId", actionId);
        AiChatMessageDO assistantMessage = insertMessage(session.getId(), "assistant", content, toJson(metadata));
        session.setLastMessageAt(LocalDateTime.now());
        sessionMapper.updateById(session);
        sendDone(emitter, assistantMessage.getId(), List.of(), usage == null ? 0L : usage, actionId);
    }

    private AiAgentActionDO requireResumableAction(Long sessionId, Long actionId, Long userId) {
        AiAgentActionDO action = actionMapper.selectById(actionId);
        if (action == null || !sessionId.equals(action.getSessionId())) {
            throw new BizException(BizCode.DATA_NOT_FOUND, "Agent 操作不存在");
        }
        if (!userId.equals(action.getUserId())) {
            throw new BizException(BizCode.FORBIDDEN, "该 Agent 操作不属于当前用户");
        }
        if (AgentActionStatus.PENDING_CONFIRM.equals(action.getStatus())) {
            throw new BizException(BizCode.OPERATION_FAIL, "请先确认或拒绝该操作，再继续对话");
        }
        return action;
    }

    /** resumedFromActionId 固定为 metadata 末键（LinkedHashMap），逗号/闭括号两种后缀覆盖键在中间/结尾的匹配，避免 123 误配 1234 */
    private AiChatMessageDO findResumedMessage(Long sessionId, Long actionId) {
        String marker = "\"resumedFromActionId\":" + actionId;
        return messageMapper.selectOne(Wrappers.<AiChatMessageDO>lambdaQuery()
                .eq(AiChatMessageDO::getSessionId, sessionId)
                .and(w -> w.like(AiChatMessageDO::getMetadataJson, marker + ",")
                        .or().like(AiChatMessageDO::getMetadataJson, marker + "}"))
                .orderByDesc(AiChatMessageDO::getId)
                .last("LIMIT 1"));
    }

    /** 原 user 消息 = 挂起计划 assistant 消息之前最近一条 user 消息（FAST_MUTATION 与 FC 挂起同构） */
    private AiChatMessageDO findOriginalUserMessage(Long sessionId, Long planMessageId) {
        if (planMessageId != null) {
            return messageMapper.selectOne(Wrappers.<AiChatMessageDO>lambdaQuery()
                    .eq(AiChatMessageDO::getSessionId, sessionId)
                    .eq(AiChatMessageDO::getRole, "user")
                    .lt(AiChatMessageDO::getId, planMessageId)
                    .orderByDesc(AiChatMessageDO::getId)
                    .last("LIMIT 1"));
        }
        // 计划消息缺失时的兜底：取最近一条 user 消息
        return messageMapper.selectOne(Wrappers.<AiChatMessageDO>lambdaQuery()
                .eq(AiChatMessageDO::getSessionId, sessionId)
                .eq(AiChatMessageDO::getRole, "user")
                .orderByDesc(AiChatMessageDO::getId)
                .last("LIMIT 1"));
    }

    /** 复用挂起时 assistant 消息 metadata 里存的 systemPrompt（知识引用等上下文不丢） */
    private String extractStoredSystemPrompt(Long messageId) {
        if (messageId == null) {
            return null;
        }
        AiChatMessageDO message = messageMapper.selectById(messageId);
        if (message == null || !StringUtils.hasText(message.getMetadataJson())) {
            return null;
        }
        try {
            String prompt = objectMapper.readTree(message.getMetadataJson()).path("systemPrompt").asText("");
            return StringUtils.hasText(prompt) ? prompt : null;
        } catch (Exception ex) {
            return null;
        }
    }

    /** 原 user 内容兜底：正则路径 params 里有 rawUserRequest（FC 挂起路径无此键，正常走消息表） */
    private String extractRawUserRequest(String paramsJson) {
        if (StringUtils.hasText(paramsJson)) {
            try {
                String raw = objectMapper.readTree(paramsJson).path("rawUserRequest").asText("");
                if (StringUtils.hasText(raw)) {
                    return raw;
                }
            } catch (Exception ignored) {
            }
        }
        return "（原请求内容缺失）";
    }

    /** 编排回调 → SSE 事件：token / toolStatus / toolResult（streamMessage 与 resumeStream 共用） */
    private AgentOrchestrator.AgentStreamListener sseListener(SseEmitter emitter, StringBuilder seen) {
        return new AgentOrchestrator.AgentStreamListener() {
            @Override
            public void onToken(String delta) {
                seen.append(delta);
                try {
                    emitter.send(SseEmitter.event().name("token").data(delta));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onToolStatus(String toolName, int round, String phase) {
                try {
                    emitter.send(SseEmitter.event().name("toolStatus").data(Map.of(
                            "toolName", toolName, "round", round, "phase", phase)));
                } catch (IOException ignored) {
                }
            }

            @Override
            public void onToolResult(String toolName, AiToolResult toolResult) {
                try {
                    emitter.send(SseEmitter.event().name("toolResult").data(toolResult));
                } catch (IOException ignored) {
                }
            }
        };
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
        if (provider == null || provider.getEnabled() == null || provider.getEnabled() != 1 || !isChatCapable(provider.getUsageType())) {
            throw new BizException(BizCode.DATA_NOT_FOUND, "Enabled provider does not exist");
        }
        return provider;
    }

    private boolean isChatCapable(String usageType) {
        return usageType == null || USAGE_CHAT.equals(usageType) || USAGE_BOTH.equals(usageType);
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
                .localFilesEnabled(session.getLocalFilesEnabled() != null && session.getLocalFilesEnabled() == 1)
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
