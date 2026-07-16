package com.aurora.ai.chat.model.req;

import com.aurora.common.base.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ChatSessionPageReq extends PageRequest {
    private Long userId;
    private Long providerId;
    private String title;
    private String model;
    private String status;
}
