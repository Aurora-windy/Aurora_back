package com.aurora.ai.tool.core;

import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AI 工具注册表（T-M2 改造）。
 * <p>
 * 内部工具（EDU 等）通过构造器注入，不可变；
 * MCP 远程工具通过 registerMcpTools/unregisterMcpTools 动态注册/注销。
 * 两类工具共用 definitions Map，FC 循环零特判。
 */
@Slf4j
@Component
public class AiToolRegistry {

    /** 内部工具（构造器注入，不可变） */
    private final Map<String, AiToolDefinition> internalDefinitions;
    /** 动态工具（MCP 远程工具，运行时注册） */
    private final Map<String, AiToolDefinition> dynamicDefinitions = new ConcurrentHashMap<>();

    public AiToolRegistry(List<AiToolDefinition> definitions) {
        this.internalDefinitions = definitions.stream()
                .collect(Collectors.toUnmodifiableMap(AiToolDefinition::getName, Function.identity()));
    }

    public AiToolDefinition get(String toolName) {
        // 优先查内部工具，再查动态工具
        AiToolDefinition definition = internalDefinitions.get(toolName);
        if (definition == null) {
            definition = dynamicDefinitions.get(toolName);
        }
        if (definition == null) {
            throw new BizException(BizCode.OPERATION_FAIL, "AI tool is not registered: " + toolName);
        }
        return definition;
    }

    public List<AiToolDefinition> list() {
        List<AiToolDefinition> all = new ArrayList<>(internalDefinitions.values());
        all.addAll(dynamicDefinitions.values());
        return all;
    }

    /**
     * 注册 MCP 远程工具（T-M2）。
     * 工具名格式：mcp.<serverCode>.<toolName>
     */
    public void registerMcpTools(List<AiToolDefinition> tools) {
        for (AiToolDefinition tool : tools) {
            dynamicDefinitions.put(tool.getName(), tool);
            log.info("tool.registry mcp registered: {}", tool.getName());
        }
    }

    /**
     * 注销指定 server 的所有工具（T-M2）。
     * 由 McpClientManager.getToolNames(serverCode) 提供工具名列表。
     */
    public void unregisterMcpTools(List<String> toolNames) {
        for (String name : toolNames) {
            AiToolDefinition removed = dynamicDefinitions.remove(name);
            if (removed != null) {
                log.info("tool.registry mcp unregistered: {}", name);
            }
        }
    }
}