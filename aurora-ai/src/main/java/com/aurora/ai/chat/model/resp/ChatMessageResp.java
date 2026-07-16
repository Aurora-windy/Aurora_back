package com.aurora.ai.chat.model.resp;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
public class ChatMessageResp implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long sessionId;
    private String role;
    private String content;
    private String metadataJson;
    private LocalDateTime createdAt;
}