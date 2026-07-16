package com.aurora.ai.chat.support;

import com.aurora.ai.knowledge.model.resp.KnowledgeCitation;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptBuilder {

    public String buildSystemPrompt(List<KnowledgeCitation> citations) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是 AURORA 业务 Agent。 ");
        prompt.append("请结合系统知识回答问题，且绝不能执行未注册工具。 ");
        prompt.append("涉及业务数据修改时，必须先生成确认计划，用户确认后才能执行。 ");
        if (citations != null && !citations.isEmpty()) {
            prompt.append("知识库引用：");
            for (KnowledgeCitation citation : citations) {
                prompt.append('[')
                        .append(citation.getDocTitle())
                        .append("#")
                        .append(citation.getChunkId())
                        .append("] ");
            }
        }
        return prompt.toString();
    }
}
