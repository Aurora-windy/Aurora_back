package com.aurora.ai.tool.edu;

import com.aurora.ai.tool.core.AiToolHandler;
import com.aurora.ai.tool.model.AiToolRequest;
import com.aurora.ai.tool.model.AiToolResult;
import com.aurora.edu.ai.EduAiToolFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EduStudentUnbindUserTool implements AiToolHandler {
    private final EduAiToolFacade facade;

    @Override
    public AiToolResult execute(AiToolRequest request) {
        facade.unbindStudentUser(ParamReader.longValue(request.getParams(), "studentId"));
        return AiToolResult.ok(null, "学生用户绑定已解除");
    }
}
