package com.aurora.ai.chat.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aurora.common.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_chat_session")
public class AiChatSessionDO extends BaseDO {
    private Long userId;
    private String title;
    private Long providerId;
    private String model;
    private String status;
    private LocalDateTime lastMessageAt;
}