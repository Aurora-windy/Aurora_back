package com.aurora.ai.chat.support;

import com.aurora.ai.chat.config.AgentProperties;
import com.aurora.ai.chat.entity.AiChatMessageDO;
import com.aurora.ai.chat.mapper.AiChatMessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 多轮记忆组装器（T1 详设 §5，T3 交付）。
 * 统一出口：[system] + 历史窗口 + [user]，快速路径归纳 / FC 循环 / resume 三通道共用。
 * 中间 tool 消息只在当轮内存不落库，历史里只有最终文本（消息表零变更）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatHistoryAssembler {

    private final AiChatMessageMapper messageMapper;
    private final AgentProperties properties;

    public List<Map<String, Object>> assemble(Long sessionId, String systemPrompt, String userContent) {
        return assemble(sessionId, systemPrompt, userContent, Set.of());
    }

    /**
     * @param excludeMessageIds 需跳过的历史消息 ID（resume 场景：原 user 消息已单独特意拼入，避免重复）
     */
    public List<Map<String, Object>> assemble(Long sessionId, String systemPrompt, String userContent,
                                              Set<Long> excludeMessageIds) {
        List<Map<String, Object>> messages = assembleContext(sessionId, systemPrompt, excludeMessageIds);
        messages.add(Map.of("role", "user", "content", userContent == null ? "" : userContent));
        return messages;
    }

    /**
     * 仅 [system] + 历史窗口，不带尾部 user 消息（T5 resume 专用：尾部由确认流的结构化消息对补齐）。
     */
    public List<Map<String, Object>> assembleContext(Long sessionId, String systemPrompt, Set<Long> excludeMessageIds) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt == null ? "" : systemPrompt));
        int historyCount = 0;
        int historyChars = 0;
        for (AiChatMessageDO message : historyWindow(sessionId, excludeMessageIds)) {
            messages.add(Map.of("role", message.getRole(), "content", message.getContent()));
            historyCount++;
            historyChars += message.getContent().length();
        }
        log.info("agent.history window messages={} chars={} sessionId={}", historyCount, historyChars, sessionId);
        return messages;
    }

    /**
     * 取会话最近 N 轮 user/assistant 文本（metadata 不进），按字符预算从新到旧纳入后正序返回。
     * 轮数上限与字符预算先到为准（char/2 ≈ token 估算）。
     */
    private List<AiChatMessageDO> historyWindow(Long sessionId, Set<Long> excludeMessageIds) {
        if (sessionId == null) {
            return List.of();
        }
        int rounds = Math.max(1, properties.getHistoryRounds());
        List<AiChatMessageDO> recent = messageMapper.selectList(new LambdaQueryWrapper<AiChatMessageDO>()
                .eq(AiChatMessageDO::getSessionId, sessionId)
                .in(AiChatMessageDO::getRole, List.of("user", "assistant"))
                .orderByDesc(AiChatMessageDO::getCreatedAt)
                .last("LIMIT " + rounds * 2));
        List<AiChatMessageDO> window = new ArrayList<>();
        int chars = 0;
        for (AiChatMessageDO message : recent) {
            if (message.getContent() == null || !StringUtils.hasText(message.getContent())) {
                continue;
            }
            if (excludeMessageIds.contains(message.getId())) {
                continue;
            }
            if (!window.isEmpty() && chars + message.getContent().length() > properties.getHistoryMaxChars()) {
                break;
            }
            window.add(message);
            chars += message.getContent().length();
        }
        // 倒序取出，正序拼回
        java.util.Collections.reverse(window);
        return window;
    }
}
