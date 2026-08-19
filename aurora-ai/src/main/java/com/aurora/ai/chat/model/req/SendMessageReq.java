package com.aurora.ai.chat.model.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class SendMessageReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    private String content;

    /** 是否调用知识库检索，默认开启 */
    private Boolean useKnowledgeBase = true;
}