package com.aurora.ai.chat.support;

import com.aurora.ai.knowledge.model.resp.KnowledgeCitation;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptBuilder {

    public String buildSystemPrompt(List<KnowledgeCitation> citations) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are AURORA business Agent. ");
        prompt.append("Answer with system knowledge and never execute unregistered tools. ");
        prompt.append("Business mutations must create a confirmation plan before execution. ");
        if (citations != null && !citations.isEmpty()) {
            prompt.append("Citations: ");
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