package com.aurora.ai.tool.edu;

import com.aurora.ai.tool.core.AiToolHandler;
import com.aurora.ai.tool.model.AiToolRequest;
import com.aurora.ai.tool.model.AiToolResult;
import com.aurora.edu.ai.EduAiToolFacade;
import com.aurora.edu.course.model.req.CoursePageReq;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EduCourseSearchTool implements AiToolHandler {
    private final EduAiToolFacade facade;

    @Override
    public AiToolResult execute(AiToolRequest request) {
        CoursePageReq req = new CoursePageReq();
        req.setCourseCode(ParamReader.string(request.getParams(), "courseCode"));
        req.setName(ParamReader.string(request.getParams(), "name"));
        req.setTeacherId(ParamReader.longValue(request.getParams(), "teacherId"));
        req.setCategory(ParamReader.intValue(request.getParams(), "category"));
        req.setStatus(ParamReader.intValue(request.getParams(), "status"));
        return AiToolResult.ok(facade.searchCourses(req), "课程查询已完成");
    }
}
