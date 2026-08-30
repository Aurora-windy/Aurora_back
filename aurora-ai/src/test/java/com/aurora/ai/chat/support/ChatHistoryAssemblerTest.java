package com.aurora.ai.chat.support;

import com.aurora.ai.chat.config.AgentProperties;
import com.aurora.ai.chat.entity.AiChatMessageDO;
import com.aurora.ai.chat.mapper.AiChatMessageMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T3 验收：多轮记忆窗口组装（正序、字符预算从新到旧、排除 ID、空会话）。
 * 轮数上限由 SQL LIMIT 实现，属数据库层行为，单测不覆盖（日志可见窗口大小兜底）。
 * T5 增补 assembleContext（resume 专用：无尾部 user）。
 */
class ChatHistoryAssemblerTest {

    private final AiChatMessageMapper messageMapper = mock(AiChatMessageMapper.class);
    private final AgentProperties properties = new AgentProperties();
    private final ChatHistoryAssembler assembler = new ChatHistoryAssembler(messageMapper, properties);

    private AiChatMessageDO message(Long id, String role, String content, int minutesAgo) {
        AiChatMessageDO message = new AiChatMessageDO();
        message.setId(id);
        message.setSessionId(1L);
        message.setRole(role);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now().minusMinutes(minutesAgo));
        return message;
    }

    /**
     * 取指定位置的消息体。断言需要具体键值类型——直接对 Map&lt;?, ?&gt; 调 containsEntry
     * 会因通配符捕获导致类型推断失败（编译期报错），故统一转成 Map&lt;Object, Object&gt;。
     */
    @SuppressWarnings("unchecked")
    private static Map<Object, Object> messageAt(List<?> messages, int index) {
        return (Map<Object, Object>) messages.get(index);
    }

    @Test
    void assemble_systemFirst_historyChronological_userLast() {
        when(messageMapper.selectList(any())).thenReturn(List.of(
                message(4L, "assistant", "答2", 1),
                message(3L, "user", "问2", 2),
                message(2L, "assistant", "答1", 3),
                message(1L, "user", "问1", 4)));

        List<?> messages = assembler.assemble(1L, "SYS", "新问题");

        assertThat(messages).hasSize(6);
        assertThat(messageAt(messages, 0)).containsEntry("role", "system").containsEntry("content", "SYS");
        // 倒序取出的历史要正序拼回：问1 答1 问2 答2
        assertThat(messageAt(messages, 1)).containsEntry("content", "问1");
        assertThat(messageAt(messages, 2)).containsEntry("content", "答1");
        assertThat(messageAt(messages, 3)).containsEntry("content", "问2");
        assertThat(messageAt(messages, 4)).containsEntry("content", "答2");
        assertThat(messageAt(messages, 5)).containsEntry("role", "user").containsEntry("content", "新问题");
    }

    @Test
    void charBudget_dropsOldest_keepsAtLeastNewest() {
        properties.setHistoryMaxChars(10);
        when(messageMapper.selectList(any())).thenReturn(List.of(
                message(3L, "assistant", "12345678", 1),   // 8 字符
                message(2L, "user", "12345", 2),           // 5 字符 → 累计 13 > 10，截断
                message(1L, "user", "更旧", 3)));

        List<?> messages = assembler.assemble(1L, "SYS", "新问题");

        assertThat(messages).hasSize(3); // system + 仅最新一条历史 + user
        assertThat(messageAt(messages, 1)).containsEntry("content", "12345678");
    }

    @Test
    void charBudget_newestAloneOverBudget_stillIncluded() {
        properties.setHistoryMaxChars(2);
        when(messageMapper.selectList(any())).thenReturn(List.of(
                message(2L, "assistant", "超长但必须保留最新", 1),
                message(1L, "user", "旧", 2)));

        List<?> messages = assembler.assemble(1L, "SYS", "新问题");

        assertThat(messages).hasSize(3);
        assertThat(messageAt(messages, 1)).containsEntry("content", "超长但必须保留最新");
    }

    @Test
    void excludeMessageIds_skipped() {
        when(messageMapper.selectList(any())).thenReturn(List.of(
                message(3L, "assistant", "答", 1),
                message(2L, "user", "被排除的原问题", 2),
                message(1L, "user", "更早", 3)));

        List<?> messages = assembler.assemble(1L, "SYS", "继续", Set.of(2L));

        assertThat(messages).hasSize(4); // system + 更早 + 答 + user（2 号被跳过）
        assertThat(messages.stream()
                .map(m -> (Map<?, ?>) m)
                .map(m -> (Object) m.get("content"))
                .toList())
                .doesNotContain("被排除的原问题");
    }

    @Test
    void blankContent_skipped() {
        when(messageMapper.selectList(any())).thenReturn(List.of(
                message(3L, "assistant", "  ", 1),
                message(2L, "user", "有效", 2)));

        List<?> messages = assembler.assemble(1L, "SYS", "新问题");

        assertThat(messages).hasSize(3);
        assertThat(messageAt(messages, 1)).containsEntry("content", "有效");
    }

    @Test
    void nullSession_or_emptyHistory_systemAndUserOnly() {
        when(messageMapper.selectList(any())).thenReturn(List.of());
        assertThat(assembler.assemble(null, "SYS", "问")).hasSize(2);
        assertThat(assembler.assemble(1L, "SYS", "问")).hasSize(2);
    }

    @Test
    void assembleContext_systemAndWindowOnly_noTrailingUser() {
        when(messageMapper.selectList(any())).thenReturn(List.of(
                message(2L, "assistant", "答", 1),
                message(1L, "user", "问", 2)));

        List<?> messages = assembler.assembleContext(1L, "SYS", Set.of(99L));

        // resume 专用：[system] + 历史窗口，尾部 user 由确认流的结构化消息对补齐
        assertThat(messages).hasSize(3);
        assertThat(messageAt(messages, 1)).containsEntry("role", "user").containsEntry("content", "问");
        assertThat(messageAt(messages, 2)).containsEntry("role", "assistant");
    }
}
