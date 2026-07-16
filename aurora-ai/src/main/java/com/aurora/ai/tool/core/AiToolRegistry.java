package com.aurora.ai.tool.core;

import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AiToolRegistry {

    private final Map<String, AiToolDefinition> definitions;

    public AiToolRegistry(List<AiToolDefinition> definitions) {
        this.definitions = definitions.stream().collect(Collectors.toUnmodifiableMap(AiToolDefinition::getName, Function.identity()));
    }

    public AiToolDefinition get(String toolName) {
        AiToolDefinition definition = definitions.get(toolName);
        if (definition == null) {
            throw new BizException(BizCode.OPERATION_FAIL, "AI tool is not registered: " + toolName);
        }
        return definition;
    }

    public List<AiToolDefinition> list() {
        return List.copyOf(definitions.values());
    }
}