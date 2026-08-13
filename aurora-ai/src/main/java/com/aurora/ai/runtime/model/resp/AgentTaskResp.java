package com.aurora.ai.runtime.model.resp;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** Agent 任务对外响应，避免暴露内部失败细节。 */
@Data
@Builder
public class AgentTaskResp {
    private Long id;
    private Long sessionId;
    private Long userId;
    private String taskType;
    private String inputJson;
    private String state;
    private String idempotencyKey;
    private Integer retryCount;
    private Integer maxRetries;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime cancelledAt;
    private String failureCode;
    private String failureMessage;
    private LocalDateTime createTime;
}
