package com.aurora.ai.tool.core;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AiToolDefinition {
    private String name;
    private String description;
    private String permissionCode;
    private Boolean mutation;
    /** @deprecated 声明式 {@link #paramSchema} 取代反射式参数类（parameterClass 实际全部为 Map.class，无反射价值）。字段保留仅为兼容，不再赋值。 */
    @Deprecated
    private Class<?> parameterClass;
    /** 声明式参数 schema：FC tools 的 JSON Schema 单一数据源（内部工具声明，MCP 工具走 {@link #toolsSchemaJson}） */
    private List<ParamField> paramSchema;
    /** mutation 工具必填：挂起确认时的风险提示文案 */
    private String riskNote;
    /** 工具需要的会话能力；为空表示无需额外能力。 */
    private String requiredCapability;
    /**
     * MCP 远程工具专用：server 返回的 inputSchema 本就是完整 JSON Schema，直接缓存于此。
     * {@link AiToolSchemaGenerator} 优先取本字段，缺失才由 paramSchema 构建（双来源单出口，见 T1 详设 §13）。
     */
    private String toolsSchemaJson;
    private AiToolHandler handler;

    /** 单个参数的声明式描述，经 {@link AiToolSchemaGenerator} 转为 JSON Schema property */
    public record ParamField(String name, ParamType type, boolean required, String description) {
    }

    public enum ParamType {
        STRING,
        INTEGER,
        LONG,
        BOOLEAN,
        /** ISO 本地日期时间，无时区（ParamReader#dateTime 直接 LocalDateTime.parse），映射为 string + format=date-time */
        DATETIME
    }
}
