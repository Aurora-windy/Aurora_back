package com.aurora.ai.audit.model.req;

import com.aurora.common.base.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AiToolCallLogPageReq extends PageRequest {
    private Long sessionId;
    private Long actionId;
    private Long userId;
    private Long providerId;
    private String toolName;
    private String permissionCode;
    private Integer success;
}
