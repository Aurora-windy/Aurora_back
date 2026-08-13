package com.aurora.ai.runtime.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 创建 Agent 任务的请求参数。 */
@Data
public class CreateAgentTaskReq {
    private Long sessionId;

    @NotBlank(message = "taskType must not be blank")
    @Size(max = 100, message = "taskType length must not exceed 100")
    private String taskType;

    private String inputJson;

    @Size(max = 128, message = "idempotencyKey length must not exceed 128")
    private String idempotencyKey;

    private Integer maxRetries;
}
