package com.aurora.ai.tool.edu;

import com.aurora.ai.tool.core.AiToolHandler;
import com.aurora.ai.tool.model.AiToolRequest;
import com.aurora.ai.tool.model.AiToolResult;
import com.aurora.edu.ai.EduAiToolFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EduSelectionDropForStudentTool implements AiToolHandler {
    private final EduAiToolFacade facade;

    @Override
    public AiToolResult execute(AiToolRequest request) {
        facade.dropCourseForStudent(ParamReader.longValue(request.getParams(), "studentId"), ParamReader.longValue(request.getParams(), "courseId"));
        return AiToolResult.ok(null, "Student course dropped");
    }
}