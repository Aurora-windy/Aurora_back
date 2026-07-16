package com.aurora.ai.chat.support;

import com.aurora.ai.tool.model.AiToolRequest;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class QueryToolIntentParser {

    private static final Pattern STUDENT_NO_PATTERN = Pattern.compile("(?:studentNo|\\u5b66\\u53f7)\\s*[:\\uFF1A#]?\\s*([A-Za-z0-9_-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COURSE_CODE_PATTERN = Pattern.compile("(?:courseCode|\\u8bfe\\u7a0b\\u7f16\\u7801)\\s*[:\\uFF1A#]?\\s*([A-Za-z0-9_-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("(?:studentId|\\u5b66\\u751fID|\\u5b66\\u751fid)\\s*[:\\uFF1A#]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    public QueryIntent parse(Long sessionId, Long userId, String userContent) {
        if (userContent == null) {
            return null;
        }
        String text = userContent.trim();
        String lower = text.toLowerCase();
        if (containsAny(text, lower, "\u9009\u8bfe", "selection") && containsAny(text, lower, "\u5b66\u751f", "student")) {
            Long studentId = extractLong(STUDENT_ID_PATTERN, text);
            if (studentId == null) {
                return null;
            }
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("studentId", studentId);
            return intent("edu.selection.byStudent", params, sessionId, userId);
        }
        if (containsAny(text, lower, "\u53ef\u7ed1\u5b9a", "bindable") || containsAny(text, lower, "\u7ed1\u5b9a\u8d26\u53f7", "bind account")) {
            Map<String, Object> params = new LinkedHashMap<>();
            String keyword = removeQueryWords(text);
            if (!keyword.isBlank()) {
                params.put("keyword", keyword);
            }
            return intent("edu.student.bindableUsers", params, sessionId, userId);
        }        if (containsAny(text, lower, "\u8bfe\u7a0b", "course")) {
            Map<String, Object> params = new LinkedHashMap<>();
            String courseCode = extractString(COURSE_CODE_PATTERN, text);
            if (courseCode != null) {
                params.put("courseCode", courseCode);
            } else {
                String keyword = removeQueryWords(text);
                if (!keyword.isBlank()) {
                    params.put("name", keyword);
                }
            }
            return intent("edu.course.search", params, sessionId, userId);
        }
        if (containsAny(text, lower, "\u5b66\u751f", "student")) {
            Map<String, Object> params = new LinkedHashMap<>();
            String studentNo = extractString(STUDENT_NO_PATTERN, text);
            if (studentNo != null) {
                params.put("studentNo", studentNo);
            } else {
                String keyword = removeQueryWords(text);
                if (!keyword.isBlank()) {
                    params.put("name", keyword);
                }
            }
            return intent("edu.student.search", params, sessionId, userId);
        }
        return null;
    }

    private QueryIntent intent(String toolName, Map<String, Object> params, Long sessionId, Long userId) {
        return QueryIntent.builder()
                .toolName(toolName)
                .request(AiToolRequest.builder()
                        .sessionId(sessionId)
                        .userId(userId)
                        .toolName(toolName)
                        .params(params)
                        .build())
                .build();
    }

    private boolean containsAny(String text, String lower, String chinese, String english) {
        return text.contains(chinese) || lower.contains(english.toLowerCase());
    }

    private String extractString(Pattern pattern, String content) {
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }

    private Long extractLong(Pattern pattern, String content) {
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? Long.valueOf(matcher.group(1)) : null;
    }

    private String removeQueryWords(String text) {
        return text.replace("\u67e5\u8be2", "")
                .replace("\u67e5\u627e", "")
                .replace("\u641c\u7d22", "")
                .replace("\u8bfe\u7a0b", "")
                .replace("\u5b66\u751f", "")
                .replace("\u9009\u8bfe", "")
                .replace("\u53ef\u7ed1\u5b9a", "")
                .replace("\u8d26\u53f7", "")
                .replace("course", "")
                .replace("student", "")
                .replace("selection", "")
                .replace("search", "")
                .trim();
    }

    @Data
    @Builder
    public static class QueryIntent {
        private String toolName;
        private AiToolRequest request;
    }
}