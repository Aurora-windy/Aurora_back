package com.aurora.ai.chat.support;

import com.aurora.ai.agent.entity.AiAgentActionDO;
import com.aurora.ai.agent.support.ActionPlanBuilder;
import com.aurora.ai.chat.entity.AiChatSessionDO;
import com.aurora.ai.chat.mapper.AiChatSessionMapper;
import com.aurora.ai.knowledge.graph.Neo4jGraphService;
import com.aurora.ai.knowledge.model.resp.KnowledgeCitation;
import com.aurora.ai.knowledge.service.KnowledgeRetrievalService;
import com.aurora.ai.provider.client.OpenAiClientFactory;
import com.aurora.ai.provider.entity.AiModelProviderDO;
import com.aurora.ai.provider.mapper.AiModelProviderMapper;
import com.aurora.ai.tool.core.AiToolExecutor;
import com.aurora.ai.tool.model.AiToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class AgentOrchestrator {

    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final PromptBuilder promptBuilder;
    private final ActionPlanBuilder actionPlanBuilder;
    private final QueryToolIntentParser queryToolIntentParser;
    private final AiToolExecutor toolExecutor;
    private final ObjectMapper objectMapper;
    private final AiChatSessionMapper sessionMapper;
    private final AiModelProviderMapper providerMapper;
    private final OpenAiClientFactory openAiClientFactory;
    private final Neo4jGraphService neo4jGraphService;

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
        AiChatSessionDO session = sessionMapper.selectById(sessionId);
        if (session == null || session.getProviderId() == null) {
            return "未选择模型，无法生成回复。请在左侧选择模型后新建会话。";
        }
        AiModelProviderDO provider = providerMapper.selectById(session.getProviderId());
        if (provider == null || provider.getEnabled() == null || provider.getEnabled() != 1) {
            return "当前会话绑定的模型供应商未启用，请检查 AI Provider 配置。";
        }
        if (!StringUtils.hasText(provider.getModel())) {
            return "当前模型供应商未配置对话模型。";
        }
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userContent));
        try {
            Double temperature = provider.getTemperature() == null ? null : provider.getTemperature().doubleValue();
            OpenAiClientFactory.ChatResult result =
                    openAiClientFactory.chatCompletionResult(provider, messages, temperature, provider.getMaxTokens());
            if (result.totalTokens() != null && result.totalTokens() > 0) {
                draft.setTotalTokens((long) result.totalTokens());
            }
            return result.content();
        } catch (Exception ex) {
            return "模型调用失败：" + ex.getMessage();
        }
    }

    private List<KnowledgeCitation> safeSearch(String userContent) {
        try {
            return knowledgeRetrievalService.search(userContent, 5);
        } catch (Exception ex) {
            return List.of();
        }
    }

    /**
     * 流式回答前置准备：复用 draft 的检索 / 意图解析逻辑，先算出 systemPrompt、citations，
     * 以及是否为「待确认操作」或「工具查询」这类不走 LLM 流式分支的特殊答案。
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

    /** 若启用了 Neo4j 知识图谱，则把查询实体的邻居关系作为补充上下文拼进 system prompt */
    private String appendGraphContext(String systemPrompt, String userContent) {
        if (!neo4jGraphService.isEnabled()) {
            return systemPrompt;
        }
        return neo4jGraphService.retrieveContext(userContent)
                .map(ctx -> systemPrompt + "\n\n# 知识图谱补充上下文\n" + ctx)
                .orElse(systemPrompt);
    }

    public void streamCallLlm(Long sessionId, String systemPrompt, String userContent,
                              Consumer<String> onToken, Consumer<Long> onUsage) {
        AiChatSessionDO session = sessionMapper.selectById(sessionId);
        if (session == null || session.getProviderId() == null) {
            if (onToken != null) onToken.accept("未选择模型，无法生成回复。请在左侧选择模型后新建会话。");
            return;
        }
        AiModelProviderDO provider = providerMapper.selectById(session.getProviderId());
        if (provider == null || provider.getEnabled() == null || provider.getEnabled() != 1) {
            if (onToken != null) onToken.accept("当前会话绑定的模型供应商未启用，请检查 AI Provider 配置。");
            return;
        }
        if (!StringUtils.hasText(provider.getModel())) {
            if (onToken != null) onToken.accept("当前模型供应商未配置对话模型。");
            return;
        }
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userContent));
        try {
            Double temperature = provider.getTemperature() == null ? null : provider.getTemperature().doubleValue();
            openAiClientFactory.streamChatCompletion(provider, messages, temperature, provider.getMaxTokens(),
                    onToken,
                    usage -> { if (onUsage != null) onUsage.accept(usage.totalTokens() == null ? 0L : (long) usage.totalTokens()); });
        } catch (Exception ex) {
            if (onToken != null) onToken.accept("模型调用失败：" + ex.getMessage());
        }
    }

    @Data
    @Builder
    public static class StreamContext {
        private List<KnowledgeCitation> citations;
        private String systemPrompt;
        private AiAgentActionDO pendingAction;
        private QueryToolIntentParser.QueryIntent queryIntent;
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
}
