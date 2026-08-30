package com.aurora.ai.tool.core;

import com.aurora.ai.tool.core.AiToolDefinition.ParamField;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具定义 → OpenAI FC tools 条目的唯一出口（T1 详设 §2.3 / §13.1）。
 * 双来源：MCP 工具的 toolsSchemaJson（完整 JSON Schema）优先；内部工具由 paramSchema 声明构建。
 */
@Component
@RequiredArgsConstructor
public class AiToolSchemaGenerator {

    private final ObjectMapper objectMapper;

    /**
     * OpenAI 兼容 API 的 function.name 仅允许 {@code [a-zA-Z0-9_-]+}（DeepSeek 官方实测拒绝点号），
     * 注册表点号名上线时映射为双下划线（edu.student.search → edu__student__search）。
     * 单射约束：注册表名不含 __，MCP 前缀（mcp_<server>_）用单下划线，__ 只会由 . 产生。
     */
    public static String toWireName(String registryName) {
        return registryName == null ? null : registryName.replace(".", "__");
    }

    /** {@link #toWireName(String)} 的逆映射：模型 tool_calls 里的 wire 名还原为注册表名 */
    public static String fromWireName(String wireName) {
        return wireName == null ? null : wireName.replace("__", ".");
    }

    /** 生成单个 tools 数组条目：{@code {"type":"function","function":{name,description,parameters}}} */
    public Map<String, Object> generate(AiToolDefinition definition) {
        Map<String, Object> parameters;
        if (StringUtils.hasText(definition.getToolsSchemaJson())) {
            parameters = parseOverride(definition);
        } else {
            parameters = buildFromParamSchema(definition.getParamSchema());
        }
        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", toWireName(definition.getName()));
        function.put("description", definition.getDescription());
        function.put("parameters", parameters);
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("type", "function");
        entry.put("function", function);
        return entry;
    }

    private Map<String, Object> parseOverride(AiToolDefinition definition) {
        try {
            return objectMapper.readValue(definition.getToolsSchemaJson(),
                    new TypeReference<Map<String, Object>>() {
                    });
        } catch (Exception ex) {
            // MCP schema 非法时退回空参数对象，工具仍可见但模型不会传参——宁可少参不可整体失败
            return buildFromParamSchema(null);
        }
    }

    private Map<String, Object> buildFromParamSchema(List<ParamField> fields) {
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        if (fields != null) {
            for (ParamField field : fields) {
                properties.put(field.name(), propertyOf(field));
                if (field.required()) {
                    required.add(field.name());
                }
            }
        }
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", required);
        return parameters;
    }

    private Map<String, Object> propertyOf(ParamField field) {
        Map<String, Object> property = new LinkedHashMap<>();
        switch (field.type()) {
            case STRING -> property.put("type", "string");
            case INTEGER, LONG -> property.put("type", "integer");
            case BOOLEAN -> property.put("type", "boolean");
            case DATETIME -> {
                property.put("type", "string");
                property.put("format", "date-time");
            }
        }
        property.put("description", field.description());
        return property;
    }
}
