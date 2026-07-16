package com.aurora.ai.agent.support;

import com.aurora.ai.agent.entity.AiAgentActionDO;
import com.aurora.ai.agent.model.resp.ActionResp;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class ActionPlanBuilder {

    private static final Pattern COURSE_ID_PATTERN = Pattern.compile("(?:courseId|\\u8bfe\\u7a0bID|\\u8bfe\\u7a0bid|\\u8bfe\\u7a0b)\\s*[:\\uFF1A#]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("(?:studentId|\\u5b66\\u751fID|\\u5b66\\u751fid|\\u5b66\\u751f)\\s*[:\\uFF1A#]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern USER_ID_PATTERN = Pattern.compile("(?:userId|\\u7528\\u6237ID|\\u7528\\u6237id|\\u8d26\\u53f7)\\s*[:\\uFF1A#]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CAPACITY_PATTERN = Pattern.compile("(?:capacity|\\u5bb9\\u91cf|\\u540d\\u989d)\\s*(?:to|=|:|\\uFF1A|\\u6539\\u4e3a|\\u8bbe\\u7f6e\\u4e3a|\\u8c03\\u6574\\u4e3a)?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern STATUS_PATTERN = Pattern.compile("(?:status|\\u72b6\\u6001)\\s*(?:to|=|:|\\uFF1A|\\u6539\\u4e3a|\\u8bbe\\u7f6e\\u4e3a)?\\s*(0|1|enable|disable|enabled|disabled|on|off|\\u542f\\u7528|\\u7981\\u7528)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ANY_NUMBER_PATTERN = Pattern.compile("(\\d+)");

    private final ObjectMapper objectMapper;

    public AiAgentActionDO tryBuildPendingAction(Long sessionId, Long userId, String userContent) {
        if (userContent == null) {
            return null;
        }
        for (ActionFactory factory : new ActionFactory[]{
                this::tryBuildCourseCapacityAction,
                this::tryBuildCourseStatusAction,
                this::tryBuildStudentBindAction,
                this::tryBuildStudentUnbindAction,
                this::tryBuildSelectionDropAction
        }) {
            AiAgentActionDO action = factory.build(sessionId, userId, userContent);
            if (action != null) {
                return action;
            }
        }
        if (!hasMutationIntent(userContent)) {
            return null;
        }
        AiAgentActionDO action = baseAction(sessionId, userId);
        action.setToolName("pending.edu.tool");
        action.setPlanSummary("检测到可能的 EDU 修改请求，但暂未匹配到受支持的确定性工具。可用示例：courseId 123 capacity 60；courseId 123 status 1；studentId 1 userId 2 bind；studentId 1 courseId 2 drop。");
        action.setRiskSummary("当前修改意图暂不支持执行。只有匹配到已注册工具且参数校验通过后，才会变更 EDU 数据。");
        action.setParamsJson(toJson(Map.of("rawUserRequest", userContent)));
        return action;
    }

    public ActionResp toResp(AiAgentActionDO action) {
        if (action == null) {
            return null;
        }
        return ActionResp.builder()
                .id(action.getId())
                .sessionId(action.getSessionId())
                .messageId(action.getMessageId())
                .actionType(action.getActionType())
                .toolName(action.getToolName())
                .planSummary(action.getPlanSummary())
                .paramsJson(action.getParamsJson())
                .riskSummary(action.getRiskSummary())
                .status(action.getStatus())
                .resultSummary(action.getResultSummary())
                .errorMessage(action.getErrorMessage())
                .confirmedAt(action.getConfirmedAt())
                .executedAt(action.getExecutedAt())
                .build();
    }

    private AiAgentActionDO tryBuildCourseCapacityAction(Long sessionId, Long userId, String userContent) {
        String text = userContent.trim();
        if (!(text.contains("\u5bb9\u91cf") || text.contains("\u540d\u989d") || text.toLowerCase().contains("capacity"))) {
            return null;
        }
        Long courseId = extractLong(COURSE_ID_PATTERN, text);
        Integer capacity = extractInteger(CAPACITY_PATTERN, text);
        NumberPair pair = extractNumberPair(text);
        if (courseId == null) {
            courseId = pair.first() == null ? null : pair.first().longValue();
        }
        if (capacity == null) {
            capacity = pair.second();
        }
        if (courseId == null || capacity == null) {
            return null;
        }
        Map<String, Object> params = orderedParams(userContent);
        params.put("courseId", courseId);
        params.put("capacity", capacity);
        return action(sessionId, userId, "edu.course.updateCapacity", params,
                "计划：将课程 " + courseId + " 的容量调整为 " + capacity + "。",
                "风险：容量不能低于已选人数；执行时会复用 RBAC 权限和 EDU 服务校验。");
    }

    private AiAgentActionDO tryBuildCourseStatusAction(Long sessionId, Long userId, String userContent) {
        String text = userContent.trim();
        String lower = text.toLowerCase();
        if (!(text.contains("\u72b6\u6001") || lower.contains("status") || lower.contains("enable") || lower.contains("disable") || text.contains("\u542f\u7528") || text.contains("\u7981\u7528"))) {
            return null;
        }
        Long courseId = extractLong(COURSE_ID_PATTERN, text);
        Integer status = extractStatus(text);
        if (courseId == null || status == null) {
            return null;
        }
        Map<String, Object> params = orderedParams(userContent);
        params.put("courseId", courseId);
        params.put("status", status);
        return action(sessionId, userId, "edu.course.updateStatus", params,
                "计划：将课程 " + courseId + " 的状态调整为 " + status + "。",
                "风险：课程状态必须为 0 或 1；执行时会复用 RBAC 权限和 EDU 服务校验。");
    }

    private AiAgentActionDO tryBuildStudentBindAction(Long sessionId, Long userId, String userContent) {
        String text = userContent.trim();
        String lower = text.toLowerCase();
        if (!(lower.contains("bind") || text.contains("\u7ed1\u5b9a")) || lower.contains("unbind") || text.contains("\u89e3\u7ed1")) {
            return null;
        }
        Long studentId = extractLong(STUDENT_ID_PATTERN, text);
        Long targetUserId = extractLong(USER_ID_PATTERN, text);
        if (studentId == null || targetUserId == null) {
            return null;
        }
        Map<String, Object> params = orderedParams(userContent);
        params.put("studentId", studentId);
        params.put("userId", targetUserId);
        return action(sessionId, userId, "edu.student.bindUser", params,
                "计划：将学生 " + studentId + " 绑定到用户 " + targetUserId + "。",
                "风险：目标用户必须存在、已启用、具备学生角色，且未绑定其他学生档案。");
    }

    private AiAgentActionDO tryBuildStudentUnbindAction(Long sessionId, Long userId, String userContent) {
        String text = userContent.trim();
        String lower = text.toLowerCase();
        if (!(lower.contains("unbind") || text.contains("\u89e3\u7ed1"))) {
            return null;
        }
        Long studentId = extractLong(STUDENT_ID_PATTERN, text);
        if (studentId == null) {
            return null;
        }
        Map<String, Object> params = orderedParams(userContent);
        params.put("studentId", studentId);
        return action(sessionId, userId, "edu.student.unbindUser", params,
                "计划：解除学生 " + studentId + " 的用户绑定。",
                "风险：确认后该学生档案将不再关联登录账号。");
    }

    private AiAgentActionDO tryBuildSelectionDropAction(Long sessionId, Long userId, String userContent) {
        String text = userContent.trim();
        String lower = text.toLowerCase();
        if (!(lower.contains("drop") || text.contains("\u9000\u8bfe"))) {
            return null;
        }
        Long studentId = extractLong(STUDENT_ID_PATTERN, text);
        Long courseId = extractLong(COURSE_ID_PATTERN, text);
        if (studentId == null || courseId == null) {
            return null;
        }
        Map<String, Object> params = orderedParams(userContent);
        params.put("studentId", studentId);
        params.put("courseId", courseId);
        return action(sessionId, userId, "edu.selection.dropForStudent", params,
                "计划：为学生 " + studentId + " 退选课程 " + courseId + "。",
                "风险：选课记录会标记为已退课，课程已选人数会通过 EDU 服务同步减少。");
    }

    private AiAgentActionDO action(Long sessionId, Long userId, String toolName, Map<String, Object> params, String plan, String risk) {
        AiAgentActionDO action = baseAction(sessionId, userId);
        action.setToolName(toolName);
        action.setPlanSummary(plan);
        action.setRiskSummary(risk);
        action.setParamsJson(toJson(params));
        return action;
    }

    private Map<String, Object> orderedParams(String rawUserRequest) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("rawUserRequest", rawUserRequest);
        return params;
    }

    private AiAgentActionDO baseAction(Long sessionId, Long userId) {
        AiAgentActionDO action = new AiAgentActionDO();
        action.setSessionId(sessionId);
        action.setUserId(userId);
        action.setActionType("MUTATION_PLAN");
        action.setStatus(AgentActionStatus.PENDING_CONFIRM);
        return action;
    }

    private boolean hasMutationIntent(String userContent) {
        String text = userContent.trim();
        String lower = text.toLowerCase();
        return text.contains("\u4fee\u6539")
                || text.contains("\u53d8\u66f4")
                || text.contains("\u8c03\u6574")
                || text.contains("\u7ed1\u5b9a")
                || text.contains("\u89e3\u7ed1")
                || text.contains("\u9000\u8bfe")
                || lower.contains("update")
                || lower.contains("change")
                || lower.contains("bind")
                || lower.contains("drop");
    }

    private Long extractLong(Pattern pattern, String content) {
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? Long.valueOf(matcher.group(1)) : null;
    }

    private Integer extractInteger(Pattern pattern, String content) {
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }

    private Integer extractStatus(String content) {
        Matcher matcher = STATUS_PATTERN.matcher(content);
        if (!matcher.find()) {
            return null;
        }
        String value = matcher.group(1).toLowerCase();
        if ("1".equals(value) || "enable".equals(value) || "enabled".equals(value) || "on".equals(value) || "\u542f\u7528".equals(value)) {
            return 1;
        }
        return 0;
    }

    private NumberPair extractNumberPair(String content) {
        Matcher matcher = ANY_NUMBER_PATTERN.matcher(content);
        Integer first = null;
        Integer second = null;
        if (matcher.find()) {
            first = Integer.valueOf(matcher.group(1));
        }
        if (matcher.find()) {
            second = Integer.valueOf(matcher.group(1));
        }
        return new NumberPair(first, second);
    }

    private String toJson(Map<String, ?> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private interface ActionFactory {
        AiAgentActionDO build(Long sessionId, Long userId, String userContent);
    }

    private record NumberPair(Integer first, Integer second) {
    }
}
