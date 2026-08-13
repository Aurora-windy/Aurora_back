package com.aurora.ai.runtime.service;

/** Agent 运行链路 Trace 服务。 */
public interface AgentTraceService {

    /** 创建执行 span，并返回可用于嵌套关联的 Trace ID。 */
    Long start(Long taskId, Long parentTraceId, String spanType, String spanName, String inputSummary);

    /** 结束 span 并保存受控的输出摘要与错误码。 */
    void finish(Long traceId, String state, String outputSummary, String errorCode);
}
