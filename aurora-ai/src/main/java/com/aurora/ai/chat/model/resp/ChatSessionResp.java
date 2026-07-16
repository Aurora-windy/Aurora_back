package com.aurora.ai.chat.model.resp;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
public class ChatSessionResp implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String title;
    private Long providerId;
    private String model;
    private String status;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createTime;
}