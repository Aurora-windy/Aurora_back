package com.aurora.ai.chat.support;

import com.aurora.ai.agent.entity.AiAgentActionDO;
import com.aurora.ai.agent.support.ActionPlanBuilder;
import com.aurora.ai.agent.support.AgentActionStatus;
import com.aurora.ai.chat.config.AgentProperties;
import com.aurora.ai.chat.entity.AiChatSessionDO;
import com.aurora.ai.chat.mapper.AiChatSessionMapper;
import com.aurora.ai.knowledge.graph.Neo4jGraphService;
import com.aurora.ai.knowledge.model.resp.KnowledgeCitation;
import com.aurora.ai.knowledge.service.KnowledgeRetrievalService;
import com.aurora.ai.provider.client.OpenAiClientFactory;
import com.aurora.ai.provider.entity.AiModelProviderDO;
import com.aurora.ai.provider.mapper.AiModelProviderMapper;
import com.aurora.ai.tool.core.AiToolDefinition;
import com.aurora.ai.tool.core.AiToolExecutor;
import com.aurora.ai.tool.core.AiToolRegistry;
import com.aurora.ai.tool.core.AiToolSchemaGenerator;
import com.aurora.ai.tool.model.AiToolRequest;
import com.aurora.ai.tool.model.AiToolResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Agent 2.0 编排器（T1 详设 §4，T4 交付）。
 *
 * 三通道：
 * ① FAST_MUTATION —— 正则确定性命中修改意图 → 挂起待确认（service 层直接处理，不经此类流式）；
 * ② FAST_QUERY   —— 正则确定性命中查询意图 → 执行工具 + LLM 流式归纳（可关，关则静态摘要文案）；
 * ③ FC           —— Function Calling 循环：模型发起 tool_calls → 查询类当场执行、结果以 role:tool 回拼 →
 *                    下一轮继续，直到纯文本终答 / max rounds / token 预算 → 强制不带 tools 终结。
 *
 * mutation 工具模型永远只能「发起」：立即挂起 AiAgentActionDO（PENDING_CONFIRM）返回，由用户确认后执行（HITL）。
 * 降级链：FC 失败且零产出（无 token、无工具执行）→ FC_FALLBACK 纯聊天；已有产出 → 尝试强制收敛，再失败才上抛标中断。
 * 中间 tool 消息只在当轮内存不落库；工具轨迹摘要随结果返回，由 service 落 assistant metadata。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentOrchestrator {

    private static final String PATH_FAST_QUERY = "FAST_QUERY";
    private static final String PATH_FC = "FC";
    private static final String PATH_FC_FALLBACK = "FC_FALLBACK";
    private static final String PATH_PLAIN = "PLAIN";

    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final PromptBuilder promptBuilder;
    private final ActionPlanBuilder actionPlanBuilder;
    private final QueryToolIntentParser queryToolIntentParser;
    private final AiToolExecutor toolExecutor;
    private final AiToolRegistry toolRegistry;
    private final AiToolSchemaGenerator toolSchemaGenerator;
    private final ChatHistoryAssembler historyAssembler;
    private final AgentProperties properties;
    private final ObjectMapper objectMapper;
    private final AiChatSessionMapper sessionMapper;
    private final AiModelProviderMapper providerMapper;
    private final OpenAiClientFactory openAiClientFactory;
    private final Neo4jGraphService neo4jGraphService;

    // ==================== 非流式（draft） ====================

    public AgentDraft draft(Long sessionId, Long userId, String userContent, Boolean useKnowledgeBase) {
        List<KnowledgeCitation> citations = Boolean.FALSE.equals(useKnowledgeBase) ? List.of() : safeSearch(userContent);
        String systemPrompt = appendGraphContext(promptBuilder.buildSystemPrompt(citations), userContent);
        AiAgentActionDO pendingAction = actionPlanBuilder.tryBuildPendingAction(sessionId, userId, userContent);
        if (pendingAction != null) {
            return AgentDraft.builder()
                    .systemPrompt(systemPrompt)
                    .assistantContent("已识别到 EDU 数据修改请求，并生成待确认操作计划。确认前不会变更任何 EDU 数据。")
                    .citations(citations)
                    .pendingAction(pendingAction)
                    .build();
        }
        QueryToolIntentParser.QueryIntent queryIntent = queryToolIntentParser.parse(sessionId, userId, userContent);
        if (queryIntent != null) {
            AiToolResult result = toolExecutor.execute(queryIntent.getRequest());
            return AgentDraft.builder()
                    .systemPrompt(systemPrompt)
                    .assistantContent(toQueryAssistantContent(queryIntent.getToolName(), result))
                    .citations(citations)
                    .toolResult(result)
                    .build();
        }
        AgentDraft draft = AgentDraft.builder()
                .systemPrompt(systemPrompt)
                .citations(citations)
                .build();
        draft.setAssistantContent(callLlm(sessionId, systemPrompt, userContent, draft));
        return draft;
    }

    private String callLlm(Long sessionId, String systemPrompt, String userContent, AgentDraft draft) {
        ProviderResolution resolution = resolveProvider(sessionId);
        if (resolution.errorMessage() != null) {
            return resolution.errorMessage();
        }
        List<Map<String, Object>> messages = historyAssembler.assemble(sessionId, systemPrompt, userContent);
        try {
            OpenAiClientFactory.ChatResult result = openAiClientFactory.chatCompletionResult(
                    resolution.provider(), messages, temperature(resolution.provider()), resolution.provider().getMaxTokens());
            if (result.totalTokens() != null && result.totalTokens() > 0) {
                draft.setTotalTokens((long) result.totalTokens());
            }
            return result.content();
        } catch (Exception ex) {
            return "模型调用失败：" + ex.getMessage();
        }
    }

    // ==================== 流式编排（T4 核心） ====================

    /**
     * 流式回答前置准备：复用 draft 的检索 / 意图解析逻辑，先算出 systemPrompt、citations，
     * 以及是否为「待确认操作」（FAST_MUTATION，service 直接处理）或「工具查询」（FAST_QUERY）。
     */
    public StreamContext prepare(Long sessionId, Long userId, String userContent, Boolean useKnowledgeBase) {
        List<KnowledgeCitation> citations = Boolean.FALSE.equals(useKnowledgeBase) ? List.of() : safeSearch(userContent);
        String systemPrompt = appendGraphContext(promptBuilder.buildSystemPrompt(citations), userContent);
        AiAgentActionDO pendingAction = actionPlanBuilder.tryBuildPendingAction(sessionId, userId, userContent);
        QueryToolIntentParser.QueryIntent queryIntent = queryToolIntentParser.parse(sessionId, userId, userContent);
        return StreamContext.builder()
                .citations(citations)
                .systemPrompt(systemPrompt)
                .pendingAction(pendingAction)
                .queryIntent(queryIntent)
                .build();
    }

    /**
     * 流式对话主入口（FAST_MUTATION 之外的通道）。
     * 所有 token / 工具状态 / 工具结果都经 {@link AgentStreamListener} 回调，SSE 适配由调用方实现。
     */
    public AgentRunResult streamConversation(Long sessionId, Long userId, String userContent,
                                             StreamContext ctx, AgentStreamListener listener) {
        if (ctx.getQueryIntent() != null) {
            return runFastQuery(sessionId, userContent, ctx, listener);
        }
        if (properties.isFcEnabled()) {
            ProviderResolution resolution = resolveProvider(sessionId);
            if (resolution.errorMessage() != null) {
                return runPlainChat(sessionId, userContent, ctx, listener, PATH_PLAIN, resolution.errorMessage());
            }
            List<Map<String, Object>> messages = historyAssembler.assemble(sessionId, ctx.getSystemPrompt(), userContent);
            return fcLoopWithFallback(resolution.provider(), sessionId, userId, messages, listener, PATH_FC, PATH_FC_FALLBACK);
        }
        return runPlainChat(sessionId, userContent, ctx, listener, PATH_PLAIN);
    }

    /** 快速路径查询：确定性正则命中 → 执行工具 → LLM 流式归纳（关掉或无 provider 时退静态摘要文案） */
    private AgentRunResult runFastQuery(Long sessionId, String userContent, StreamContext ctx, AgentStreamListener listener) {
        String toolName = ctx.getQueryIntent().getToolName();
        AiToolRequest request = ctx.getQueryIntent().getRequest();
        listener.onToolStatus(toolName, 1, "start");
        AiToolResult result = toolExecutor.execute(request);
        boolean ok = Boolean.TRUE.equals(result.getSuccess());
        listener.onToolStatus(toolName, 1, ok ? "success" : "failed");
        listener.onToolResult(toolName, result);
        List<Map<String, Object>> toolTrace = new ArrayList<>();
        toolTrace.add(trace(1, toolName, ok ? "success" : "failed"));

        String staticContent = toQueryAssistantContent(toolName, result);
        ProviderResolution resolution = resolveProvider(sessionId);
        if (!properties.isFastpathSummarize() || resolution.errorMessage() != null) {
            listener.onToken(staticContent);
            log.info("agent.path={} sessionId={} summarized=false", PATH_FAST_QUERY, sessionId);
            return AgentRunResult.builder()
                    .content(staticContent).toolTrace(toolTrace).totalTokens(0).path(PATH_FAST_QUERY)
                    .build();
        }

        // 合规的 assistant(tool_calls)+tool 消息对（带 tool_call_id），避免裸 tool 消息被严格校验的供应商拒绝
        List<Map<String, Object>> messages = historyAssembler.assemble(sessionId, ctx.getSystemPrompt(), userContent);
        messages.add(assistantToolCallsMessage(List.of(new OpenAiClientFactory.ToolCall("call_fastpath", AiToolSchemaGenerator.toWireName(toolName), "{}"))));
        messages.add(toolMessage("call_fastpath", toToolContent(result)));

        StringBuilder content = new StringBuilder();
        long[] tokens = {0};
        try {
            // tools 必须随请求带上：消息里有 assistant(tool_calls)+tool 合成对，部分中转校验"有 tool 消息必须带 tools"，
            // 缺失直接 400（联调实测）。tool_choice=auto + 结果已回拼，正常情况模型不会再发起调用
            openAiClientFactory.streamChatCompletion(resolution.provider(), messages,
                    temperature(resolution.provider()), resolution.provider().getMaxTokens(), buildToolSchemas(),
                    delta -> {
                        content.append(delta);
                        listener.onToken(delta);
                    },
                    usage -> tokens[0] += totalTokensOf(usage), null);
            if (content.length() == 0) {
                // 零产出兜底：静态摘要
                content.append(staticContent);
                listener.onToken(staticContent);
            }
        } catch (Exception ex) {
            log.warn("agent.fastquery summarize failed sessionId={} tool={}", sessionId, toolName, ex);
            if (content.length() == 0) {
                content.append(staticContent);
                listener.onToken(staticContent);
            }
            String tail = "\n（结果归纳中断：" + ex.getMessage() + "）";
            content.append(tail);
            listener.onToken(tail);
        }
        log.info("agent.path={} sessionId={} summarized=true tool={} tokens={}", PATH_FAST_QUERY, sessionId, toolName, tokens[0]);
        return AgentRunResult.builder()
                .content(content.toString()).toolTrace(toolTrace).totalTokens(tokens[0]).path(PATH_FAST_QUERY)
                .build();
    }

    /**
     * FC 循环（T1 §4 伪代码的落地实现）。messages 由调用方组装（普通对话走历史窗口，resume 走 action 表重建）。
     */
    private AgentRunResult fcLoopWithFallback(AiModelProviderDO provider, Long sessionId, Long userId,
                                              List<Map<String, Object>> messages, AgentStreamListener listener,
                                              String path, String fallbackPath) {
        List<Map<String, Object>> tools = buildToolSchemas();
        StringBuilder content = new StringBuilder();
        long[] tokens = {0};
        List<Map<String, Object>> toolTrace = new ArrayList<>();

        try {
            for (int round = 1; round <= properties.getMaxRounds(); round++) {
                List<OpenAiClientFactory.ToolCall> toolCalls = new ArrayList<>();
                openAiClientFactory.streamChatCompletion(provider, messages, temperature(provider), provider.getMaxTokens(), tools,
                        delta -> {
                            content.append(delta);
                            listener.onToken(delta);
                        },
                        usage -> tokens[0] += totalTokensOf(usage),
                        toolCalls::addAll);

                if (toolCalls.isEmpty()) {
                    // 纯文本终答，已流式发出
                    log.info("agent.path={} sessionId={} rounds={} toolCalls={} tokens={}",
                            path, sessionId, round, toolTrace.size(), tokens[0]);
                    return AgentRunResult.builder()
                            .content(content.toString()).toolTrace(toolTrace).totalTokens(tokens[0]).path(path)
                            .build();
                }

                // 本轮模型发起了工具调用：先回拼 assistant(tool_calls) 协议消息
                messages.add(assistantToolCallsMessage(toolCalls));
                for (OpenAiClientFactory.ToolCall call : toolCalls) {
                    // 模型回传的是 wire 名（toWireName 映射），注册表查找/展示/审计一律用还原后的真实名
                    String displayName = AiToolSchemaGenerator.fromWireName(call.name());
                    AiToolDefinition definition = lookupQuietly(displayName);
                    if (definition == null) {
                        messages.add(toolMessage(call.id(), "未知工具：" + displayName + "，只能调用系统提供的工具。"));
                        continue;
                    }
                    if (Boolean.TRUE.equals(definition.getMutation())) {
                        // mutation：模型只能发起，立即挂起等用户确认（本轮到此为止）
                        listener.onToolStatus(displayName, round, "pending");
                        toolTrace.add(trace(round, displayName, "pending"));
                        String suspendText = "\n\n已发起修改操作 " + displayName
                                + "，系统生成待确认计划。请在下方确认或拒绝，确认前任何数据不会变更。";
                        content.append(suspendText);
                        listener.onToken(suspendText);
                        log.info("agent.path={} sessionId={} round={} suspended tool={} args={}",
                                path, sessionId, round, displayName, call.argumentsJson());
                        return AgentRunResult.builder()
                                .content(content.toString()).pendingAction(buildFcPendingAction(sessionId, userId, definition, call))
                                .toolTrace(toolTrace).totalTokens(tokens[0]).path(path)
                                .build();
                    }
                    // 查询类：当场执行，结果以 role:tool 回拼
                    listener.onToolStatus(displayName, round, "start");
                    AiToolResult result = executeQueryTool(call, sessionId, userId);
                    boolean ok = Boolean.TRUE.equals(result.getSuccess());
                    listener.onToolStatus(displayName, round, ok ? "success" : "failed");
                    listener.onToolResult(displayName, result);
                    toolTrace.add(trace(round, displayName, ok ? "success" : "failed"));
                    messages.add(toolMessage(call.id(), toToolContent(result)));
                }

                // 轮数上限 / token 预算先到为准 → 强制不带 tools 终结一轮，让模型基于已有工具结果作答
                long estimated = tokens[0] + content.length() / 2;
                boolean overBudget = properties.getTokenBudget() > 0 && estimated >= properties.getTokenBudget();
                if (overBudget || round == properties.getMaxRounds()) {
                    log.info("agent.fc force-finalize sessionId={} round={} overBudget={} toolCalls={}",
                            sessionId, round, overBudget, toolTrace.size());
                    openAiClientFactory.streamChatCompletion(provider, messages, temperature(provider), provider.getMaxTokens(), null,
                            delta -> {
                                content.append(delta);
                                listener.onToken(delta);
                            },
                            usage -> tokens[0] += totalTokensOf(usage), null);
                    return AgentRunResult.builder()
                            .content(content.toString()).toolTrace(toolTrace).totalTokens(tokens[0]).path(path)
                            .build();
                }
            }
        } catch (Exception ex) {
            if (content.length() == 0 && toolTrace.isEmpty()) {
                // 零产出才整体降级为纯聊天，避免内容重复（T1 §3.4 原则）
                log.warn("agent.fc error, fallback to plain chat sessionId={}", sessionId, ex);
                return plainStreamMessages(provider, sessionId, messages, listener, fallbackPath);
            }
            // 已有工具结果/部分输出：先尝试强制收敛一次，再失败才上抛（service 标中断）
            log.warn("agent.fc error mid-loop, force-finalize sessionId={}", sessionId, ex);
            try {
                openAiClientFactory.streamChatCompletion(provider, messages, temperature(provider), provider.getMaxTokens(), null,
                        delta -> {
                            content.append(delta);
                            listener.onToken(delta);
                        },
                        usage -> tokens[0] += totalTokensOf(usage), null);
                return AgentRunResult.builder()
                        .content(content.toString()).toolTrace(toolTrace).totalTokens(tokens[0]).path(path)
                        .build();
            } catch (Exception ex2) {
                ex2.addSuppressed(ex);
                throw ex2;
            }
        }
        // 循环正常走完（round==maxRounds 已在循环内强制收敛），此处仅为编译器可达性兜底
        return AgentRunResult.builder()
                .content(content.toString()).toolTrace(toolTrace).totalTokens(tokens[0]).path(path)
                .build();
    }

    /** 纯聊天流式（现状行为；FC 关闭 / FC 零产出降级两条来路） */
    private AgentRunResult runPlainChat(Long sessionId, String userContent, StreamContext ctx,
                                        AgentStreamListener listener, String path) {
        return runPlainChat(sessionId, userContent, ctx, listener, path, null);
    }

    private AgentRunResult runPlainChat(Long sessionId, String userContent, StreamContext ctx,
                                        AgentStreamListener listener, String path, String presetError) {
        if (presetError == null) {
            ProviderResolution resolution = resolveProvider(sessionId);
            presetError = resolution.errorMessage();
            if (presetError == null) {
                List<Map<String, Object>> messages = historyAssembler.assemble(sessionId, ctx.getSystemPrompt(), userContent);
                return plainStreamMessages(resolution.provider(), sessionId, messages, listener, path);
            }
        }
        listener.onToken(presetError);
        log.info("agent.path={} sessionId={} tokens=0", path, sessionId);
        return AgentRunResult.builder()
                .content(presetError).totalTokens(0).path(path)
                .build();
    }

    /** 在既有 messages 上做不带工具的纯流式（FC 关闭 / 零产出降级 / resume 降级共用），失败文案同样走 onToken */
    private AgentRunResult plainStreamMessages(AiModelProviderDO provider, Long sessionId,
                                               List<Map<String, Object>> messages, AgentStreamListener listener, String path) {
        StringBuilder content = new StringBuilder();
        long[] tokens = {0};
        try {
            openAiClientFactory.streamChatCompletion(provider, messages, temperature(provider), provider.getMaxTokens(), null,
                    delta -> {
                        content.append(delta);
                        listener.onToken(delta);
                    },
                    usage -> tokens[0] += totalTokensOf(usage), null);
        } catch (Exception ex) {
            String errorText = "模型调用失败：" + ex.getMessage();
            content.append(errorText);
            listener.onToken(errorText);
        }
        log.info("agent.path={} sessionId={} tokens={}", path, sessionId, tokens[0]);
        return AgentRunResult.builder()
                .content(content.toString()).totalTokens(tokens[0]).path(path)
                .build();
    }

    // ==================== 确认流续聊（T5，T1 §7.2） ====================

    /**
     * 确认/拒绝后续聊：从 action 表重建上下文跑 FC 循环（可继续调用查询工具核实结果）。
     * messages = [system] + 历史(去重原 user 与计划 assistant) + user(原请求)
     *           + assistant(tool_calls: 发起过的修改) + tool(执行结果/拒绝说明) + user(请继续)。
     * confirm 是事务性 REST、resume 是表现层流式，二者分离（设计决策：resume 失败不影响已发生的确认）。
     */
    public AgentRunResult streamResume(Long sessionId, Long userId, AiAgentActionDO action,
                                       ResumeContext resumeCtx, AgentStreamListener listener) {
        ProviderResolution resolution = resolveProvider(sessionId);
        if (resolution.errorMessage() != null) {
            listener.onToken(resolution.errorMessage());
            log.info("agent.path=FC_RESUME sessionId={} actionId={} providerError", sessionId, action.getId());
            return AgentRunResult.builder()
                    .content(resolution.errorMessage()).totalTokens(0).path("FC_RESUME")
                    .build();
        }
        List<Map<String, Object>> messages = buildResumeMessages(sessionId, action, resumeCtx);
        if (!properties.isFcEnabled()) {
            return plainStreamMessages(resolution.provider(), sessionId, messages, listener, "FC_RESUME");
        }
        return fcLoopWithFallback(resolution.provider(), sessionId, userId, messages, listener, "FC_RESUME", "FC_RESUME_FALLBACK");
    }

    private List<Map<String, Object>> buildResumeMessages(Long sessionId, AiAgentActionDO action, ResumeContext resumeCtx) {
        List<Map<String, Object>> messages = historyAssembler.assembleContext(sessionId, resumeCtx.systemPrompt(), resumeCtx.excludeMessageIds());
        messages.add(Map.of("role", "user", "content", resumeCtx.originalUserContent() == null ? "" : resumeCtx.originalUserContent()));
        // 协议合规的合成对：assistant(tool_calls) 携计划摘要 + tool(tool_call_id) 携执行结果
        String callId = "call_resume_" + action.getId();
        messages.add(assistantToolCallsMessage(
                "已生成操作计划：" + (StringUtils.hasText(action.getPlanSummary()) ? action.getPlanSummary() : action.getToolName()),
                List.of(new OpenAiClientFactory.ToolCall(callId, AiToolSchemaGenerator.toWireName(action.getToolName()), canonicalParamsJson(action.getParamsJson())))));
        messages.add(toolMessage(callId, resumeToolContent(action)));
        messages.add(Map.of("role", "user", "content", "请根据以上执行结果，继续完成对用户的回复。"));
        return messages;
    }

    /** action 终态 → 回拼给模型的 tool 结果文本 */
    private String resumeToolContent(AiAgentActionDO action) {
        if (AgentActionStatus.EXECUTED.equals(action.getStatus())) {
            return "操作已执行成功：" + (StringUtils.hasText(action.getResultSummary()) ? action.getResultSummary() : "无详细结果。");
        }
        if (AgentActionStatus.FAILED.equals(action.getStatus())) {
            String reason = StringUtils.hasText(action.getErrorMessage()) ? action.getErrorMessage()
                    : (StringUtils.hasText(action.getResultSummary()) ? action.getResultSummary() : "未知原因");
            return "操作执行失败：" + reason + "。请如实向用户说明失败原因，不要假装成功。";
        }
        if (AgentActionStatus.REJECTED.equals(action.getStatus())) {
            return "用户已拒绝该操作，数据未发生任何变更。请如实向用户说明。";
        }
        return "操作当前状态：" + action.getStatus() + "。";
    }

    // ==================== FC 支撑 ====================

    private List<Map<String, Object>> buildToolSchemas() {
        return toolRegistry.list().stream()
                .map(toolSchemaGenerator::generate)
                .toList();
    }

    private AiToolDefinition lookupQuietly(String toolName) {
        try {
            return toolRegistry.get(toolName);
        } catch (Exception ex) {
            return null;
        }
    }

    /** 执行模型发起的查询工具：arguments 必须是合法 JSON 对象，非法时以失败结果回拼给模型自行纠正 */
    private AiToolResult executeQueryTool(OpenAiClientFactory.ToolCall call, Long sessionId, Long userId) {
        Map<String, Object> params;
        try {
            params = objectMapper.readValue(call.argumentsJson(), new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            return AiToolResult.fail("INVALID_ARGUMENTS", "模型参数不是合法 JSON 对象");
        }
        AiToolRequest request = AiToolRequest.builder()
                .sessionId(sessionId)
                .userId(userId)
                .toolName(AiToolSchemaGenerator.fromWireName(call.name()))
                .params(params)
                .build();
        try {
            return toolExecutor.execute(request);
        } catch (Exception ex) {
            // executor 内部已兜业务异常，这里只防 registry.get 等意外
            return AiToolResult.fail("TOOL_ERROR", ex.getMessage());
        }
    }

    /** FC 挂起动作：actionType/status 与正则快速路径完全一致，确认流（confirm/execute）零改动复用 */
    private AiAgentActionDO buildFcPendingAction(Long sessionId, Long userId, AiToolDefinition definition,
                                                 OpenAiClientFactory.ToolCall call) {
        AiAgentActionDO action = new AiAgentActionDO();
        action.setSessionId(sessionId);
        action.setUserId(userId);
        action.setActionType("MUTATION_PLAN");
        action.setStatus(AgentActionStatus.PENDING_CONFIRM);
        action.setToolName(definition.getName());
        action.setPlanSummary("模型发起 " + definition.getName() + "：" + abbreviate(call.argumentsJson(), 200));
        action.setRiskSummary(StringUtils.hasText(definition.getRiskNote())
                ? definition.getRiskNote()
                : "模型发起的修改操作，用户确认后才会执行。");
        action.setParamsJson(canonicalParamsJson(call.argumentsJson()));
        return action;
    }

    /** 模型 arguments → 规范 JSON（读一遍再写一遍，保证落库合法）；非法 JSON 降级为 rawModelArguments 包装 */
    private String canonicalParamsJson(String argumentsJson) {
        try {
            return objectMapper.writeValueAsString(
                    objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {
                    }));
        } catch (Exception ex) {
            return toJson(Map.of("rawModelArguments", argumentsJson == null ? "" : argumentsJson));
        }
    }

    /** OpenAI FC 协议：assistant 消息携带 tool_calls 数组（content 为模型当轮的伴随文本，无则空串） */
    private Map<String, Object> assistantToolCallsMessage(List<OpenAiClientFactory.ToolCall> toolCalls) {
        return assistantToolCallsMessage("", toolCalls);
    }

    private Map<String, Object> assistantToolCallsMessage(String content, List<OpenAiClientFactory.ToolCall> toolCalls) {
        List<Map<String, Object>> calls = new ArrayList<>();
        for (OpenAiClientFactory.ToolCall call : toolCalls) {
            Map<String, Object> function = new LinkedHashMap<>();
            function.put("name", call.name());
            function.put("arguments", call.argumentsJson());
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", call.id());
            entry.put("type", "function");
            entry.put("function", function);
            calls.add(entry);
        }
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "assistant");
        message.put("content", content == null ? "" : content);
        message.put("tool_calls", calls);
        return message;
    }

    /** OpenAI FC 协议：tool 消息以 tool_call_id 关联，内容为工具结果文本 */
    private Map<String, Object> toolMessage(String toolCallId, String content) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "tool");
        message.put("tool_call_id", toolCallId);
        message.put("content", content);
        return message;
    }

    /** 工具结果 → 回拼给模型的文本（截断到 toolResultMaxChars，防超长结果撑爆上下文） */
    private String toToolContent(AiToolResult result) {
        if (!Boolean.TRUE.equals(result.getSuccess())) {
            return "工具执行失败：" + result.getErrorMessage();
        }
        try {
            String json = objectMapper.writeValueAsString(result.getData());
            if (json.isEmpty()) {
                return "{}";
            }
            int max = Math.max(200, properties.getToolResultMaxChars());
            return json.length() > max ? json.substring(0, max) + "...(已截断)" : json;
        } catch (Exception ex) {
            return String.valueOf(result.getData());
        }
    }

    private Map<String, Object> trace(int round, String toolName, String status) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("round", round);
        entry.put("toolName", toolName);
        entry.put("status", status);
        return entry;
    }

    private String abbreviate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() > max ? text.substring(0, max) + "..." : text;
    }

    private ProviderResolution resolveProvider(Long sessionId) {
        AiChatSessionDO session = sessionMapper.selectById(sessionId);
        if (session == null || session.getProviderId() == null) {
            return new ProviderResolution(null, "未选择模型，无法生成回复。请在左侧选择模型后新建会话。");
        }
        AiModelProviderDO provider = providerMapper.selectById(session.getProviderId());
        if (provider == null || provider.getEnabled() == null || provider.getEnabled() != 1) {
            return new ProviderResolution(null, "当前会话绑定的模型供应商未启用，请检查 AI Provider 配置。");
        }
        if (!StringUtils.hasText(provider.getModel())) {
            return new ProviderResolution(null, "当前模型供应商未配置对话模型。");
        }
        return new ProviderResolution(provider, null);
    }

    private Double temperature(AiModelProviderDO provider) {
        return provider.getTemperature() == null ? null : provider.getTemperature().doubleValue();
    }

    private long totalTokensOf(OpenAiClientFactory.StreamUsage usage) {
        return usage == null || usage.totalTokens() == null ? 0L : usage.totalTokens();
    }

    private List<KnowledgeCitation> safeSearch(String userContent) {
        try {
            return knowledgeRetrievalService.search(userContent, 5);
        } catch (Exception ex) {
            return List.of();
        }
    }

    /** 若启用了 Neo4j 知识图谱，则把查询实体的邻居关系作为补充上下文拼进 system prompt */
    private String appendGraphContext(String systemPrompt, String userContent) {
        if (!neo4jGraphService.isEnabled()) {
            return systemPrompt;
        }
        return neo4jGraphService.retrieveContext(userContent)
                .map(ctx -> systemPrompt + "\n\n# 知识图谱补充上下文\n" + ctx)
                .orElse(systemPrompt);
    }

    private String toQueryAssistantContent(String toolName, AiToolResult result) {
        if (!Boolean.TRUE.equals(result.getSuccess())) {
            return "工具调用失败：" + result.getErrorMessage();
        }
        return "已执行查询工具 " + toolName + "，结果摘要：" + summarize(result.getData());
    }

    private String summarize(Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            return json.length() > 1200 ? json.substring(0, 1200) + "..." : json;
        } catch (Exception ex) {
            return String.valueOf(data);
        }
    }

    private String toJson(Map<String, ?> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return "{}";
        }
    }

    // ==================== 模型 ====================

    /** 流式编排回调：token 增量 / 工具状态（页面步骤提示）/ 工具结果 */
    public interface AgentStreamListener {
        void onToken(String delta);

        default void onToolStatus(String toolName, int round, String phase) {
        }

        default void onToolResult(String toolName, AiToolResult result) {
        }
    }

    /** 一次流式编排的产出：最终内容 + 可能的挂起动作（FC mutation）+ 工具轨迹（落 assistant metadata） */
    @Data
    @Builder
    public static class AgentRunResult {
        private String content;
        private AiAgentActionDO pendingAction;
        private List<Map<String, Object>> toolTrace;
        private long totalTokens;
        private String path;
    }

    /** resume 上下文：service 从消息表 / action 表重建后传入 */
    public record ResumeContext(String systemPrompt, String originalUserContent, Set<Long> excludeMessageIds) {
    }

    @Data
    @Builder
    public static class StreamContext {
        private List<KnowledgeCitation> citations;
        private String systemPrompt;
        private AiAgentActionDO pendingAction;
        private QueryToolIntentParser.QueryIntent queryIntent;
    }

    @Data
    @Builder
    public static class AgentDraft {
        private String systemPrompt;
        private String assistantContent;
        private List<KnowledgeCitation> citations;
        private AiAgentActionDO pendingAction;
        private AiToolResult toolResult;
        /** 本次调用消耗的 token 总数（仅 callLlm 分支有值），用于用量统计 */
        private Long totalTokens;
    }

    private record ProviderResolution(AiModelProviderDO provider, String errorMessage) {
    }
}
