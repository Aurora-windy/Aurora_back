package com.aurora.ai.runtime.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aurora.common.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** Agent 任务持久化实体，记录运行时生命周期和幂等信息。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_agent_task")
public class AiAgentTaskDO extends BaseDO {
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
}
