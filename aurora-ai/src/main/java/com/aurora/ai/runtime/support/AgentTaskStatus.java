package com.aurora.ai.runtime.support;

import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;

import java.util.Map;
import java.util.Set;

/** Agent 任务状态机，集中约束所有合法的生命周期迁移。 */
public final class AgentTaskStatus {

    public static final String PENDING = "PENDING";
    public static final String RUNNING = "RUNNING";
    public static final String WAITING_CONFIRMATION = "WAITING_CONFIRMATION";
    public static final String SUCCEEDED = "SUCCEEDED";
    public static final String FAILED = "FAILED";
    public static final String CANCELLED = "CANCELLED";

    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            PENDING, Set.of(RUNNING, CANCELLED),
            RUNNING, Set.of(WAITING_CONFIRMATION, SUCCEEDED, FAILED, CANCELLED),
            WAITING_CONFIRMATION, Set.of(RUNNING, CANCELLED)
    );

    private AgentTaskStatus() {
    }

    /** 校验任务状态迁移，终态和未知状态不允许继续迁移。 */
    public static void requireTransition(String from, String to) {
        if (from == null || to == null || !TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new BizException(BizCode.OPERATION_FAIL, "Agent task state transition is illegal");
        }
    }

    /** 判断状态是否已结束，结束后的任务不会再次进入执行流程。 */
    public static boolean isTerminal(String state) {
        return SUCCEEDED.equals(state) || FAILED.equals(state) || CANCELLED.equals(state);
    }
}
