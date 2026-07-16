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
        return AgentDraft.builder()
                .systemPrompt(systemPrompt)
                .assistantContent("已根据当前知识库准备回复。你也可以继续查询 EDU 学生、课程、选课记录，或发起受控 EDU 修改请求。")
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
    }
}
