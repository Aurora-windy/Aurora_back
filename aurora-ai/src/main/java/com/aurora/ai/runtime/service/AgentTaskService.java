package com.aurora.ai.runtime.service;

import com.aurora.ai.runtime.model.req.CreateAgentTaskReq;
import com.aurora.ai.runtime.model.resp.AgentTaskResp;

/** Agent 任务生命周期服务。 */
public interface AgentTaskService {

    /** 创建任务；相同用户和幂等键重复提交时返回已有任务。 */
    AgentTaskResp create(CreateAgentTaskReq req);

    /** 查询当前用户拥有的任务。 */
    AgentTaskResp getOwned(Long taskId);

    /** 按状态机规则更新任务状态，并记录迁移原因。 */
    AgentTaskResp transition(Long taskId, String targetState, String reason);

    /** 取消处于执行链路中的任务。 */
    AgentTaskResp cancel(Long taskId, String reason);

    /** 将失败任务置回待执行状态，且受最大重试次数限制。 */
    AgentTaskResp retry(Long taskId);
}
