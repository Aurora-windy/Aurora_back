package com.aurora.ai.tool.edu;

import java.time.LocalDateTime;
import java.util.Map;

final class ParamReader {
    private ParamReader() {
    }

    static String string(Map<String, Object> params, String key) {
        Object value = params == null ? null : params.get(key);
        return value == null ? null : String.valueOf(value);
    }

    static Long longValue(Map<String, Object> params, String key) {
        Object value = params == null ? null : params.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return Long.valueOf(String.valueOf(value));
    }

    static Integer intValue(Map<String, Object> params, String key) {
        Object value = params == null ? null : params.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return Integer.valueOf(String.valueOf(value));
    }

    static LocalDateTime dateTime(Map<String, Object> params, String key) {
        String value = string(params, key);
        return value == null || value.isBlank() ? null : LocalDateTime.parse(value);
    }
}