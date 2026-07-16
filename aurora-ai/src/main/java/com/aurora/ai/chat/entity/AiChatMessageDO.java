package com.aurora.ai.chat.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aurora.common.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_chat_message")
public class AiChatMessageDO extends BaseDO {
    private Long sessionId;
    private String role;
    private String content;
    private String metadataJson;
    private LocalDateTime createdAt;
}