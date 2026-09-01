package com.aurora.ai.chat.support;

import com.aurora.ai.agent.entity.AiAgentActionDO;
import com.aurora.ai.agent.support.AgentActionStatus;
import com.aurora.ai.chat.config.AgentProperties;
import com.aurora.ai.chat.entity.AiChatSessionDO;
import com.aurora.ai.chat.mapper.AiChatSessionMapper;
// 嵌套类型不适用「同包免 import」：StreamContext 是 AgentOrchestrator 的内部类，必须显式导入
import com.aurora.ai.chat.support.AgentOrchestrator.StreamContext;
import com.aurora.ai.knowledge.graph.Neo4jGraphService;
import com.aurora.ai.provider.client.OpenAiClientFactory;
import com.aurora.ai.provider.entity.AiModelProviderDO;
import com.aurora.ai.provider.mapper.AiModelProviderMapper;
import com.aurora.ai.tool.core.AiToolDefinition;
import com.aurora.ai.tool.core.AiToolExecutor;
import com.aurora.ai.tool.core.AiToolRegistry;
import com.aurora.ai.tool.core.AiToolSchemaGenerator;
import com.aurora.ai.tool.model.AiToolRequest;
import com.aurora.ai.tool.model.AiToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * T4 验收（T1 §10 用例清单）：FC 循环协议回拼 / mutation 挂起 / max rounds 兜底 /
 * 零产出降级 / 中途断流收敛 / 未知工具 / 非法参数 / 快速路径归纳。
 * 真实组件：AiToolRegistry + AiToolSchemaGenerator + PromptBuilder + ObjectMapper；mock：factory/executor/mapper/assembler。
 */
class AgentOrchestratorTest {

    private static final Long SESSION_ID = 1L;
    private static final Long USER_ID = 42L;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiToolExecutor toolExecutor = mock(AiToolExecutor.class);
    private final OpenAiClientFactory factory = mock(OpenAiClientFactory.class);
    private final AiChatSessionMapper sessionMapper = mock(AiChatSessionMapper.class);
    private final AiModelProviderMapper providerMapper = mock(AiModelProviderMapper.class);
    private final ChatHistoryAssembler historyAssembler = mock(ChatHistoryAssembler.class);

    private AgentProperties properties;
    private AgentOrchestrator orchestrator;
    private final RecordingListener listener = new RecordingListener();

    /** 捕获每次流式调用的 messages（fcLoop 全程复用同一个 list，断言其最终协议结构） */
    private final ArgumentCaptor<List<Map<String, Object>>> messagesCaptor = ArgumentCaptor.forClass(List.class);

    private static final class RecordingListener implements AgentOrchestrator.AgentStreamListener {
        final StringBuilder content = new StringBuilder();
        final List<String> statuses = new ArrayList<>();
        final List<String> results = new ArrayList<>();

        @Override
        public void onToken(String delta) {
            content.append(delta);
        }

        @Override
        public void onToolStatus(String toolName, int round, String phase) {
            statuses.add(round + ":" + toolName + ":" + phase);
        }

        @Override
        public void onToolResult(String toolName, AiToolResult result) {
            results.add(toolName);
        }
    }

    @BeforeEach
    void setUp() {
        properties = new AgentProperties();
        AiToolRegistry registry = new AiToolRegistry(List.of(
                AiToolDefinition.builder()
                        .name("edu.student.getById")
                        .description("按 ID 查学生")
                        .permissionCode("edu:student:query")
                        .mutation(false)
                        .handler(req -> AiToolResult.ok(Map.of("name", "张三"), "ok"))
                        .build(),
                AiToolDefinition.builder()
                        .name("edu.course.updateCapacity")
                        .description("改课程容量")
                        .permissionCode("edu:course:update")
                        .mutation(true)
                        .riskNote("风险：容量不能低于已选人数")
                        .handler(req -> AiToolResult.ok(null, "ok"))
                        .build()));
        orchestrator = new AgentOrchestrator(
                mock(com.aurora.ai.knowledge.service.KnowledgeRetrievalService.class),
                new PromptBuilder(),
                mock(com.aurora.ai.agent.support.ActionPlanBuilder.class),
                mock(QueryToolIntentParser.class),
                toolExecutor,
                registry,
                new AiToolSchemaGenerator(objectMapper),
                historyAssembler,
                properties,
                objectMapper,
                sessionMapper,
                providerMapper,
                factory,
                mock(Neo4jGraphService.class));

        AiChatSessionDO session = new AiChatSessionDO();
        session.setProviderId(9L);
        when(sessionMapper.selectById(SESSION_ID)).thenReturn(session);
        AiModelProviderDO provider = new AiModelProviderDO();
        provider.setEnabled(1);
        provider.setModel("deepseek-chat");
        when(providerMapper.selectById(9L)).thenReturn(provider);
        // 组装器每次返回新的可变列表（fcLoop 会在其上追加协议消息）
        when(historyAssembler.assemble(eq(SESSION_ID), anyString(), anyString()))
                .thenAnswer(inv -> new ArrayList<>(List.of(
                        Map.of("role", "system", "content", "SYS"),
                        Map.of("role", "user", "content", "Q"))));
        // resume 场景：assembleContext 只返回 [system]（无尾部 user）
        when(historyAssembler.assembleContext(eq(SESSION_ID), anyString(), any()))
                .thenAnswer(inv -> new ArrayList<>(List.of(
                        Map.of("role", "system", "content", "SYS"))));
        when(toolExecutor.execute(any()))
                .thenReturn(AiToolResult.ok(Map.of("name", "张三"), "ok"));
    }

    private StreamContext fcContext() {
        return StreamContext.builder().systemPrompt("SYS").citations(List.of()).build();
    }

    /** 模拟 DeepSeek 行为：带 tools 的调用按脚本回 tool_calls；不带 tools 的调用回纯文本 */
    private interface RoundScript {
        void play(boolean withTools, Consumer<String> onToken, Consumer<List<OpenAiClientFactory.ToolCall>> onToolCalls);
    }

    private void stubStream(RoundScript script) {
        // streamChatCompletion 是 void 方法，必须用 doAnswer 形式打桩
        org.mockito.Mockito.doAnswer(inv -> {
            boolean withTools = inv.getArgument(4) != null;
            Consumer<String> onToken = inv.getArgument(5);
            Consumer<List<OpenAiClientFactory.ToolCall>> onToolCalls = inv.getArgument(7);
            script.play(withTools, onToken, onToolCalls);
            return null;
        }).when(factory).streamChatCompletion(any(), anyList(), any(), any(), any(), any(), any(), any());
    }

    /**
     * 按调用顺序编排每轮的模型响应（第 1 次调用用第 1 个脚本，依此类推；超出则用最后一个）。
     *
     * <p>与 {@link #stubStream} 的区别：FC 循环<b>每一轮都会带 tools 参数</b>——这是实现的
     * 既定行为，也符合详设 §4 的伪代码（只有 maxRounds 用尽后的强制收敛才不带 tools）。
     * 因此不能用「是否带 tools」来区分轮次，否则桩会每轮都回 tool_calls，循环直到
     * maxRounds 才停。真实模型拿到工具结果后会直接给出文本终答，本方法才是它的正确建模。</p>
     */
    private void stubRounds(RoundScript... rounds) {
        AtomicInteger call = new AtomicInteger(0);
        org.mockito.Mockito.doAnswer(inv -> {
            boolean withTools = inv.getArgument(4) != null;
            Consumer<String> onToken = inv.getArgument(5);
            Consumer<List<OpenAiClientFactory.ToolCall>> onToolCalls = inv.getArgument(7);
            int idx = Math.min(call.getAndIncrement(), rounds.length - 1);
            rounds[idx].play(withTools, onToken, onToolCalls);
            return null;
        }).when(factory).streamChatCompletion(any(), anyList(), any(), any(), any(), any(), any(), any());
    }

    private static void emitToolCall(Consumer<List<OpenAiClientFactory.ToolCall>> onToolCalls,
                                     String id, String name, String args) {
        onToolCalls.accept(List.of(new OpenAiClientFactory.ToolCall(id, name, args)));
    }

    // ==================== FC 循环 ====================

    @Test
    void fcLoop_queryToolCall_thenFinalAnswer() {
        stubRounds(
                (withTools, onToken, onToolCalls) ->
                        emitToolCall(onToolCalls, "call_1", "edu.student.getById", "{\"studentId\":3}"),
                (withTools, onToken, onToolCalls) -> onToken.accept("学生姓名是张三"));

        AgentOrchestrator.AgentRunResult result =
                orchestrator.streamConversation(SESSION_ID, USER_ID, "查一下学号 3", fcContext(), listener);

        assertThat(result.getPath()).isEqualTo("FC");
        assertThat(result.getContent()).contains("学生姓名是张三");
        assertThat(result.getToolTrace()).hasSize(1);
        assertThat(result.getToolTrace().get(0))
                .containsEntry("round", 1)
                .containsEntry("toolName", "edu.student.getById")
                .containsEntry("status", "success");
        assertThat(listener.statuses).containsExactly(
                "1:edu.student.getById:start", "1:edu.student.getById:success");
        assertThat(listener.results).containsExactly("edu.student.getById");

        // 参数解析：模型 JSON 参数透传给 executor
        ArgumentCaptor<AiToolRequest> requestCaptor = ArgumentCaptor.forClass(AiToolRequest.class);
        verify(toolExecutor).execute(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getToolName()).isEqualTo("edu.student.getById");
        assertThat(requestCaptor.getValue().getParams()).containsEntry("studentId", 3);

        // 协议回拼：最终 messages = [system, user, assistant(tool_calls), tool(tool_call_id)]
        verify(factory, org.mockito.Mockito.times(2))
                .streamChatCompletion(any(), messagesCaptor.capture(), any(), any(), any(), any(), any(), any());
        List<Map<String, Object>> messages = messagesCaptor.getValue();
        assertThat(messages).hasSize(4);
        Map<String, Object> assistant = messages.get(2);
        assertThat(assistant).containsEntry("role", "assistant");
        assertThat((List<?>) assistant.get("tool_calls")).hasSize(1);
        Map<String, Object> toolMsg = messages.get(3);
        assertThat(toolMsg).containsEntry("role", "tool").containsEntry("tool_call_id", "call_1");
        assertThat((String) toolMsg.get("content")).contains("张三");
    }

    @Test
    void fcLoop_wireNameMappedBackToRegistryAndDisplay() {
        // DeepSeek 官方 API 场景：模型只会回 wire 名（点号已映射为双下划线），循环须还原注册表名
        stubRounds(
                (withTools, onToken, onToolCalls) ->
                        emitToolCall(onToolCalls, "call_w", "edu__student__getById", "{\"studentId\":3}"),
                (withTools, onToken, onToolCalls) -> onToken.accept("学生姓名是张三"));

        AgentOrchestrator.AgentRunResult result =
                orchestrator.streamConversation(SESSION_ID, USER_ID, "查一下学号 3", fcContext(), listener);

        assertThat(result.getPath()).isEqualTo("FC");
        assertThat(result.getToolTrace().get(0)).containsEntry("toolName", "edu.student.getById");
        assertThat(listener.statuses).containsExactly(
                "1:edu.student.getById:start", "1:edu.student.getById:success");
        ArgumentCaptor<AiToolRequest> requestCaptor = ArgumentCaptor.forClass(AiToolRequest.class);
        verify(toolExecutor).execute(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getToolName()).isEqualTo("edu.student.getById");
    }

    @Test
    void fcLoop_mutationToolCall_suspendsWithPendingAction() {
        stubStream((withTools, onToken, onToolCalls) -> emitToolCall(onToolCalls, "call_m",
                "edu.course.updateCapacity", "{\"courseId\":5,\"capacity\":60}"));

        AgentOrchestrator.AgentRunResult result =
                orchestrator.streamConversation(SESSION_ID, USER_ID, "把课程5容量改成60", fcContext(), listener);

        assertThat(result.getPath()).isEqualTo("FC");
        AiAgentActionDO action = result.getPendingAction();
        assertThat(action).isNotNull();
        assertThat(action.getActionType()).isEqualTo("MUTATION_PLAN");
        assertThat(action.getStatus()).isEqualTo("PENDING_CONFIRM");
        assertThat(action.getToolName()).isEqualTo("edu.course.updateCapacity");
        assertThat(action.getParamsJson()).contains("courseId").contains("capacity");
        assertThat(action.getRiskSummary()).contains("容量不能低于已选人数");
        // 模型只能发起：任何工具都不允许被模型直接执行
        verifyNoInteractions(toolExecutor);
        assertThat(listener.statuses).containsExactly("1:edu.course.updateCapacity:pending");
        assertThat(result.getToolTrace().get(0)).containsEntry("status", "pending");
        assertThat(result.getContent()).contains("待确认");
    }

    @Test
    void fcLoop_maxRounds_forceFinalizeWithoutTools() {
        properties.setMaxRounds(2);
        stubStream((withTools, onToken, onToolCalls) -> {
            if (withTools) {
                emitToolCall(onToolCalls, "call_" + System.nanoTime(), "edu.student.getById", "{\"studentId\":3}");
            } else {
                onToken.accept("收敛回答");
            }
        });

        AgentOrchestrator.AgentRunResult result =
                orchestrator.streamConversation(SESSION_ID, USER_ID, "反复要工具", fcContext(), listener);

        // 2 轮工具 + 1 次强制收敛 = 3 次调用，且最后一次不带 tools
        verify(factory, org.mockito.Mockito.times(3))
                .streamChatCompletion(any(), messagesCaptor.capture(), any(), any(), any(), any(), any(), any());
        assertThat(result.getContent()).contains("收敛回答");
        assertThat(result.getPath()).isEqualTo("FC");
    }

    @Test
    void fcLoop_unknownTool_feedbackToModel_andRecover() {
        stubRounds(
                (withTools, onToken, onToolCalls) -> emitToolCall(onToolCalls, "call_g", "edu.ghost.tool", "{}"),
                (withTools, onToken, onToolCalls) -> onToken.accept("改用已知工具回答"));

        AgentOrchestrator.AgentRunResult result =
                orchestrator.streamConversation(SESSION_ID, USER_ID, "调个不存在的", fcContext(), listener);

        assertThat(result.getContent()).contains("改用已知工具回答");
        verify(factory, org.mockito.Mockito.times(2))
                .streamChatCompletion(any(), messagesCaptor.capture(), any(), any(), any(), any(), any(), any());
        Map<String, Object> toolMsg = messagesCaptor.getValue().get(3);
        assertThat((String) toolMsg.get("content")).contains("未知工具");
        verifyNoInteractions(toolExecutor);
    }

    @Test
    void fcLoop_invalidArgumentsJson_toldToModelAsFailure() {
        stubRounds(
                (withTools, onToken, onToolCalls) -> emitToolCall(onToolCalls, "call_bad", "edu.student.getById", "不是JSON"),
                (withTools, onToken, onToolCalls) -> onToken.accept("重试成功"));

        AgentOrchestrator.AgentRunResult result =
                orchestrator.streamConversation(SESSION_ID, USER_ID, "参数坏了", fcContext(), listener);

        verifyNoInteractions(toolExecutor);
        assertThat(result.getToolTrace().get(0)).containsEntry("status", "failed");
        verify(factory, org.mockito.Mockito.times(2))
                .streamChatCompletion(any(), messagesCaptor.capture(), any(), any(), any(), any(), any(), any());
        Map<String, Object> toolMsg = messagesCaptor.getValue().get(3);
        assertThat((String) toolMsg.get("content")).contains("工具执行失败");
    }

    @Test
    void fcLoop_zeroOutputFailure_fallsBackToPlainChat() {
        stubStream((withTools, onToken, onToolCalls) -> {
            if (withTools) {
                throw new RuntimeException("FC 挂了");
            }
            onToken.accept("降级后的纯聊天回答");
        });

        AgentOrchestrator.AgentRunResult result =
                orchestrator.streamConversation(SESSION_ID, USER_ID, "问点别的", fcContext(), listener);

        assertThat(result.getPath()).isEqualTo("FC_FALLBACK");
        assertThat(result.getContent()).isEqualTo("降级后的纯聊天回答");
    }

    @Test
    void fcLoop_midLoopFailure_forceFinalizesWithToolContext() {
        AtomicInteger callsWithTools = new AtomicInteger();
        stubStream((withTools, onToken, onToolCalls) -> {
            if (withTools) {
                if (callsWithTools.incrementAndGet() == 1) {
                    emitToolCall(onToolCalls, "call_1", "edu.student.getById", "{\"studentId\":3}");
                } else {
                    throw new RuntimeException("第二轮断流");
                }
            } else {
                onToken.accept("基于工具结果的收敛回答");
            }
        });

        AgentOrchestrator.AgentRunResult result =
                orchestrator.streamConversation(SESSION_ID, USER_ID, "查一下", fcContext(), listener);

        // 已执行过工具：不整体降级（避免重复输出），改为强制收敛
        assertThat(result.getPath()).isEqualTo("FC");
        assertThat(result.getContent()).contains("基于工具结果的收敛回答");
        assertThat(result.getToolTrace()).hasSize(1);
    }

    @Test
    void fcLoop_finalizeAlsoFails_exceptionPropagates() {
        stubStream((withTools, onToken, onToolCalls) -> {
            if (withTools) {
                emitToolCall(onToolCalls, "call_1", "edu.student.getById", "{\"studentId\":3}");
            } else {
                throw new RuntimeException("收敛也失败");
            }
        });
        // 第 1 轮执行完工具后到 maxRounds → 强制收敛也失败 → 异常上抛（service 标中断）
        properties.setMaxRounds(1);

        assertThatThrownBy(() ->
                orchestrator.streamConversation(SESSION_ID, USER_ID, "查一下", fcContext(), listener))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("收敛也失败");
    }

    // ==================== FAST_QUERY ====================

    @Test
    void fastQuery_summarizedByLlm_withProtocolCompliantPair() {
        stubStream((withTools, onToken, onToolCalls) -> onToken.accept("归纳后的结果"));
        StreamContext ctx = StreamContext.builder()
                .systemPrompt("SYS")
                .queryIntent(QueryToolIntentParser.QueryIntent.builder()
                        .toolName("edu.student.getById")
                        .request(AiToolRequest.builder()
                                .toolName("edu.student.getById")
                                .params(Map.of("studentId", 3))
                                .build())
                        .build())
                .build();

        AgentOrchestrator.AgentRunResult result =
                orchestrator.streamConversation(SESSION_ID, USER_ID, "学号3是谁", ctx, listener);

        assertThat(result.getPath()).isEqualTo("FAST_QUERY");
        assertThat(result.getContent()).isEqualTo("归纳后的结果");
        verify(toolExecutor).execute(any());
        // 合成协议对：末两条 = assistant(tool_calls 带 id) + tool(tool_call_id 对应)
        verify(factory, org.mockito.Mockito.times(1))
                .streamChatCompletion(any(), messagesCaptor.capture(), any(), any(), any(), any(), any(), any());
        List<Map<String, Object>> messages = messagesCaptor.getValue();
        Map<String, Object> assistant = messages.get(messages.size() - 2);
        Map<String, Object> toolMsg = messages.get(messages.size() - 1);
        String callId = (String) ((List<Map<String, Object>>) assistant.get("tool_calls")).get(0).get("id");
        assertThat(toolMsg).containsEntry("tool_call_id", callId);
        assertThat((String) toolMsg.get("content")).contains("张三");
    }

    @Test
    void fastQuery_summarizeDisabled_staticContent() {
        properties.setFastpathSummarize(false);
        StreamContext ctx = StreamContext.builder()
                .systemPrompt("SYS")
                .queryIntent(QueryToolIntentParser.QueryIntent.builder()
                        .toolName("edu.student.getById")
                        .request(AiToolRequest.builder()
                                .toolName("edu.student.getById")
                                .params(Map.of("studentId", 3))
                                .build())
                        .build())
                .build();

        AgentOrchestrator.AgentRunResult result =
                orchestrator.streamConversation(SESSION_ID, USER_ID, "学号3是谁", ctx, listener);

        assertThat(result.getPath()).isEqualTo("FAST_QUERY");
        assertThat(result.getContent()).contains("已执行查询工具").contains("张三");
        verifyNoInteractions(factory);
    }

    // ==================== resume 确认流续聊（T5） ====================

    private AiAgentActionDO action(String status) {
        AiAgentActionDO action = new AiAgentActionDO();
        action.setId(7L);
        action.setSessionId(SESSION_ID);
        action.setUserId(USER_ID);
        action.setToolName("edu.course.updateCapacity");
        action.setPlanSummary("计划：将课程 5 的容量调整为 60。");
        action.setParamsJson("{\"courseId\":5,\"capacity\":60}");
        action.setStatus(status);
        return action;
    }

    @Test
    void resume_executed_rebuildsContextAndContinues() {
        stubStream((withTools, onToken, onToolCalls) -> onToken.accept("已把课程5容量改为60"));
        AiAgentActionDO action = action(AgentActionStatus.EXECUTED);
        action.setResultSummary("更新成功，影响 1 行");

        AgentOrchestrator.AgentRunResult result = orchestrator.streamResume(SESSION_ID, USER_ID, action,
                new AgentOrchestrator.ResumeContext("SYS", "把课程5容量改成60", Set.of(11L, 12L)), listener);

        assertThat(result.getPath()).isEqualTo("FC_RESUME");
        assertThat(result.getContent()).isEqualTo("已把课程5容量改为60");
        verify(factory, org.mockito.Mockito.times(1))
                .streamChatCompletion(any(), messagesCaptor.capture(), any(), any(), any(), any(), any(), any());
        // 消息组装契约（T1 §7.2）：[system, user(原请求), assistant(tool_calls+计划摘要), tool(执行结果), user(请继续)]
        List<Map<String, Object>> messages = messagesCaptor.getValue();
        assertThat(messages).hasSize(5);
        assertThat(messages.get(1)).containsEntry("role", "user").containsEntry("content", "把课程5容量改成60");
        Map<String, Object> assistant = messages.get(2);
        assertThat(assistant).containsEntry("role", "assistant");
        assertThat((String) assistant.get("content")).contains("已生成操作计划").contains("容量调整为 60");
        Map<String, Object> toolMsg = messages.get(3);
        assertThat(toolMsg).containsEntry("role", "tool").containsEntry("tool_call_id", "call_resume_7");
        assertThat((String) toolMsg.get("content")).contains("操作已执行成功").contains("更新成功，影响 1 行");
        assertThat(messages.get(4)).containsEntry("role", "user");
        assertThat((String) messages.get(4).get("content")).contains("继续");
    }

    @Test
    void resume_rejected_tellsModelNoChange() {
        stubStream((withTools, onToken, onToolCalls) -> onToken.accept("好的，未做任何变更"));
        AiAgentActionDO action = action(AgentActionStatus.REJECTED);

        orchestrator.streamResume(SESSION_ID, USER_ID, action,
                new AgentOrchestrator.ResumeContext("SYS", "把课程5容量改成60", Set.of()), listener);

        verify(factory, org.mockito.Mockito.times(1))
                .streamChatCompletion(any(), messagesCaptor.capture(), any(), any(), any(), any(), any(), any());
        Map<String, Object> toolMsg = messagesCaptor.getValue().get(3);
        assertThat((String) toolMsg.get("content")).contains("用户已拒绝").contains("未发生任何变更");
    }

    @Test
    void resume_fcDisabled_degradesToPlainStream() {
        properties.setFcEnabled(false);
        stubStream((withTools, onToken, onToolCalls) -> {
            assertThat(withTools).isFalse(); // 降级路径不带 tools
            onToken.accept("降级后的汇报");
        });

        AgentOrchestrator.AgentRunResult result = orchestrator.streamResume(SESSION_ID, USER_ID,
                action(AgentActionStatus.EXECUTED),
                new AgentOrchestrator.ResumeContext("SYS", "把课程5容量改成60", Set.of()), listener);

        assertThat(result.getPath()).isEqualTo("FC_RESUME");
        assertThat(result.getContent()).isEqualTo("降级后的汇报");
    }

    // ==================== provider 异常 ====================

    @Test
    void providerMissing_emitsErrorText_withoutCallingFactory() {
        AiChatSessionDO noProvider = new AiChatSessionDO();
        when(sessionMapper.selectById(SESSION_ID)).thenReturn(noProvider);

        AgentOrchestrator.AgentRunResult result =
                orchestrator.streamConversation(SESSION_ID, USER_ID, "你好", fcContext(), listener);

        assertThat(result.getContent()).contains("未选择模型");
        assertThat(listener.content.toString()).contains("未选择模型");
        verifyNoInteractions(factory);
    }
}
