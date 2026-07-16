package com.aurora.ai.tool.edu;

import com.aurora.ai.tool.core.AiToolHandler;
import com.aurora.ai.tool.model.AiToolRequest;
import com.aurora.ai.tool.model.AiToolResult;
import com.aurora.edu.ai.EduAiToolFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EduCourseUpdateSelectionTimeTool implements AiToolHandler {
    private final EduAiToolFacade facade;

    @Override
    public AiToolResult execute(AiToolRequest request) {
        facade.updateSelectionTime(ParamReader.longValue(request.getParams(), "courseId"), ParamReader.dateTime(request.getParams(), "startTime"), ParamReader.dateTime(request.getParams(), "endTime"));
        return AiToolResult.ok(null, "Course selection time updated");
    }
}