package com.aurora.ai.tool.core;

import com.aurora.ai.tool.model.AiToolRequest;
import com.aurora.ai.tool.model.AiToolResult;

public interface AiToolHandler {
    AiToolResult execute(AiToolRequest request);
}