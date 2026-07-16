package com.aurora.ai.chat.model.resp;

import com.aurora.ai.agent.model.resp.ActionResp;
import com.aurora.ai.knowledge.model.resp.KnowledgeCitation;
import com.aurora.ai.tool.model.AiToolResult;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
public class ChatSendResp implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private ChatMessageResp userMessage;
    private ChatMessageResp assistantMessage;
    private List<KnowledgeCitation> citations;
    private ActionResp pendingAction;
    private AiToolResult toolResult;
}