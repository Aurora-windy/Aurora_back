package com.aurora.ai.tool.edu;

import com.aurora.ai.tool.core.AiToolHandler;
import com.aurora.ai.tool.model.AiToolRequest;
import com.aurora.ai.tool.model.AiToolResult;
import com.aurora.edu.ai.EduAiToolFacade;
import com.aurora.edu.student.model.req.StudentPageReq;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EduStudentSearchTool implements AiToolHandler {
    private final EduAiToolFacade facade;

    @Override
    public AiToolResult execute(AiToolRequest request) {
        StudentPageReq req = new StudentPageReq();
        req.setName(ParamReader.string(request.getParams(), "name"));
        req.setStudentNo(ParamReader.string(request.getParams(), "studentNo"));
        req.setStatus(ParamReader.intValue(request.getParams(), "status"));
        Object result = facade.searchStudents(req);
        return AiToolResult.ok(result, "学生查询已完成");
    }
}
