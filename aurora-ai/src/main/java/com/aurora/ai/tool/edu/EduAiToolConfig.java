package com.aurora.ai.tool.edu;

import com.aurora.ai.tool.core.AiToolDefinition;
import com.aurora.ai.tool.core.AiToolDefinition.ParamField;
import com.aurora.ai.tool.core.AiToolDefinition.ParamType;
import com.aurora.ai.tool.core.AiToolHandler;
import com.aurora.common.constant.PermCodeConst;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class EduAiToolConfig {

    @Bean
    public AiToolDefinition eduStudentSearchToolDefinition(EduStudentSearchTool handler) {
        return definition("edu.student.search", "查询 EDU 学生档案。可按学号精确或姓名模糊检索，不传参数时返回学生列表（有分页/上限）。",
                PermCodeConst.Edu.Student.LIST, false, handler,
                List.of(
                        new ParamField("studentNo", ParamType.STRING, false, "学号，精确匹配"),
                        new ParamField("name", ParamType.STRING, false, "学生姓名，模糊匹配"),
                        new ParamField("status", ParamType.INTEGER, false, "学生状态过滤：0=禁用，1=启用")),
                null);
    }

    @Bean
    public AiToolDefinition eduStudentBindableUsersToolDefinition(EduStudentBindableUsersTool handler) {
        return definition("edu.student.bindableUsers", "查询可绑定到学生档案的系统用户账号列表，用于绑定前挑选目标用户。",
                PermCodeConst.Edu.Student.LIST, false, handler,
                List.of(
                        new ParamField("keyword", ParamType.STRING, false, "用户姓名/账号关键词，模糊匹配"),
                        new ParamField("currentStudentId", ParamType.INTEGER, false, "当前学生档案 ID，用于排除已绑定该档案的用户")),
                null);
    }

    @Bean
    public AiToolDefinition eduCourseSearchToolDefinition(EduCourseSearchTool handler) {
        return definition("edu.course.search", "查询 EDU 课程信息。可按课程编码精确或课程名称模糊检索。",
                PermCodeConst.Edu.Course.LIST, false, handler,
                List.of(
                        new ParamField("courseCode", ParamType.STRING, false, "课程编码，精确匹配"),
                        new ParamField("name", ParamType.STRING, false, "课程名称，模糊匹配"),
                        new ParamField("teacherId", ParamType.INTEGER, false, "授课教师用户 ID"),
                        new ParamField("category", ParamType.INTEGER, false, "课程分类 ID"),
                        new ParamField("status", ParamType.INTEGER, false, "课程状态过滤：0=下架，1=上架")),
                null);
    }

    @Bean
    public AiToolDefinition eduSelectionByStudentToolDefinition(EduSelectionByStudentTool handler) {
        return definition("edu.selection.byStudent", "查询指定学生的全部选课记录（课程名、教师、选课时间等）。",
                PermCodeConst.Edu.Course.LIST, false, handler,
                List.of(
                        new ParamField("studentId", ParamType.INTEGER, true, "学生档案 ID（必填）")),
                null);
    }

    @Bean
    public AiToolDefinition eduStudentBindUserToolDefinition(EduStudentBindUserTool handler) {
        return definition("edu.student.bindUser", "将学生档案绑定到指定系统用户账号。修改类操作，需用户确认后才会执行。",
                PermCodeConst.Edu.Student.EDIT, true, handler,
                List.of(
                        new ParamField("studentId", ParamType.INTEGER, true, "学生档案 ID（必填）"),
                        new ParamField("userId", ParamType.INTEGER, true, "目标系统用户 ID（必填）")),
                "将学生档案绑定到指定用户账号，影响该用户的 EDU 数据访问身份");
    }

    @Bean
    public AiToolDefinition eduStudentUnbindUserToolDefinition(EduStudentUnbindUserTool handler) {
        return definition("edu.student.unbindUser", "解除学生档案与用户账号的绑定。修改类操作，需用户确认后才会执行。",
                PermCodeConst.Edu.Student.EDIT, true, handler,
                List.of(
                        new ParamField("studentId", ParamType.INTEGER, true, "学生档案 ID（必填）")),
                "解除学生档案与用户账号的绑定，该用户将失去对应学生身份");
    }

    @Bean
    public AiToolDefinition eduCourseUpdateCapacityToolDefinition(EduCourseUpdateCapacityTool handler) {
        return definition("edu.course.updateCapacity", "修改课程容量上限。修改类操作，需用户确认后才会执行。",
                PermCodeConst.Edu.Course.EDIT, true, handler,
                List.of(
                        new ParamField("courseId", ParamType.INTEGER, true, "课程 ID（必填）"),
                        new ParamField("capacity", ParamType.INTEGER, true, "新的容量上限（必填，正整数）")),
                "修改课程容量上限，影响后续选课名额");
    }

    @Bean
    public AiToolDefinition eduCourseUpdateSelectionTimeToolDefinition(EduCourseUpdateSelectionTimeTool handler) {
        return definition("edu.course.updateSelectionTime", "修改课程选课开放时间窗口。修改类操作，需用户确认后才会执行。",
                PermCodeConst.Edu.Course.EDIT, true, handler,
                List.of(
                        new ParamField("courseId", ParamType.INTEGER, true, "课程 ID（必填）"),
                        new ParamField("startTime", ParamType.DATETIME, true, "选课开始时间（必填，ISO 格式如 2026-01-01T10:00:00，无时区）"),
                        new ParamField("endTime", ParamType.DATETIME, true, "选课结束时间（必填，ISO 格式如 2026-01-10T22:00:00，无时区）")),
                "修改课程选课时间窗口，影响学生可选课时间");
    }

    @Bean
    public AiToolDefinition eduCourseUpdateStatusToolDefinition(EduCourseUpdateStatusTool handler) {
        return definition("edu.course.updateStatus", "修改课程上架/下架状态。修改类操作，需用户确认后才会执行。",
                PermCodeConst.Edu.Course.EDIT, true, handler,
                List.of(
                        new ParamField("courseId", ParamType.INTEGER, true, "课程 ID（必填）"),
                        new ParamField("status", ParamType.INTEGER, true, "目标状态（必填）：0=下架，1=上架")),
                "修改课程上下架状态，影响课程可见性与可选性");
    }

    @Bean
    public AiToolDefinition eduSelectionDropForStudentToolDefinition(EduSelectionDropForStudentTool handler) {
        return definition("edu.selection.dropForStudent", "为指定学生退选指定课程。修改类操作，需用户确认后才会执行。",
                PermCodeConst.Edu.SELECTION_DROP, true, handler,
                List.of(
                        new ParamField("studentId", ParamType.INTEGER, true, "学生档案 ID（必填）"),
                        new ParamField("courseId", ParamType.INTEGER, true, "课程 ID（必填）")),
                "为指定学生退选课程，选课记录将被删除且课程容量计数回退");
    }

    private AiToolDefinition definition(String name, String description, String permissionCode, boolean mutation,
                                        AiToolHandler handler, List<ParamField> paramSchema, String riskNote) {
        return AiToolDefinition.builder()
                .name(name)
                .description(description)
                .permissionCode(permissionCode)
                .mutation(mutation)
                .paramSchema(paramSchema)
                .riskNote(riskNote)
                .handler(handler)
                .build();
    }
}
