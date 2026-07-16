package com.aurora.ai.tool.edu;

import com.aurora.ai.tool.core.AiToolHandler;
import com.aurora.ai.tool.model.AiToolRequest;
import com.aurora.ai.tool.model.AiToolResult;
import com.aurora.edu.ai.EduAiToolFacade;
import com.aurora.edu.student.model.req.StudentAccountOptionReq;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EduStudentBindableUsersTool implements AiToolHandler {
    private final EduAiToolFacade facade;

    @Override
    public AiToolResult execute(AiToolRequest request) {
        StudentAccountOptionReq req = new StudentAccountOptionReq();
        req.setKeyword(ParamReader.string(request.getParams(), "keyword"));
        req.setCurrentStudentId(ParamReader.longValue(request.getParams(), "currentStudentId"));
        return AiToolResult.ok(facade.listBindableUsers(req), "Bindable users search completed");
    }
}