package com.aurora.ai.tool.edu;

import com.aurora.ai.tool.core.AiToolHandler;
import com.aurora.ai.tool.model.AiToolRequest;
import com.aurora.ai.tool.model.AiToolResult;
import com.aurora.edu.ai.EduAiToolFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EduSelectionByStudentTool implements AiToolHandler {
    private final EduAiToolFacade facade;

    @Override
    public AiToolResult execute(AiToolRequest request) {
        Long studentId = ParamReader.longValue(request.getParams(), "studentId");
        return AiToolResult.ok(facade.listSelectionsByStudent(studentId), "学生选课记录已加载");
    }
}
