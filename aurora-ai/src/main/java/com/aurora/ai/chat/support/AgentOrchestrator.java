package com.aurora.ai.chat.support;

import com.aurora.ai.agent.entity.AiAgentActionDO;
import com.aurora.ai.agent.support.ActionPlanBuilder;
import com.aurora.ai.knowledge.model.resp.KnowledgeCitation;
import com.aurora.ai.knowledge.service.KnowledgeRetrievalService;
import com.aurora.ai.tool.core.AiToolExecutor;
import com.aurora.ai.tool.model.AiToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AgentOrchestrator {

    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final PromptBuilder promptBuilder;
    private final ActionPlanBuilder actionPlanBuilder;
    private final QueryToolIntentParser queryToolIntentParser;
    private final AiToolExecutor toolExecutor;
    private final ObjectMapper objectMapper;

    public AgentDraft draft(Long sessionId, Long userId, String userContent) {
        List<KnowledgeCitation> citations = safeSearch(userContent);
        String systemPrompt = promptBuilder.buildSystemPrompt(citations);
        AiAgentActionDO pendingAction = actionPlanBuilder.tryBuildPendingAction(sessionId, userId, userContent);
        if (pendingAction != null) {
            return AgentDraft.builder()
                    .systemPrompt(systemPrompt)
                    .assistantContent("Mutation request detected. A pending confirmation plan has been created. EDU data will not change before confirmation.")
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
        return AgentDraft.builder()
                .systemPrompt(systemPrompt)
                .assistantContent("I prepared an answer from the current knowledge base. You can also query EDU students, courses, selections, or request a controlled EDU mutation.")
                .citations(citations)
                .build();
    }

    private List<KnowledgeCitation> safeSearch(String userContent) {
        try {
            return knowledgeRetrievalService.search(userContent, 5);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String toQueryAssistantContent(String toolName, AiToolResult result) {
        if (!Boolean.TRUE.equals(result.getSuccess())) {
            return "Tool call failed: " + result.getErrorMessage();
        }
        return "Executed query tool " + toolName + ". Result summary: " + summarize(result.getData());
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
    }
}