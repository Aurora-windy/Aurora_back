package com.aurora.ai.tool.edu;

import com.aurora.ai.tool.core.AiToolDefinition;
import com.aurora.ai.tool.core.AiToolHandler;
import com.aurora.common.constant.PermCodeConst;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EduAiToolConfig {

    @Bean
    public AiToolDefinition eduStudentSearchToolDefinition(EduStudentSearchTool handler) {
        return definition("edu.student.search", "查询 EDU 学生档案", PermCodeConst.Edu.Student.LIST, false, handler);
    }

    @Bean
    public AiToolDefinition eduStudentBindableUsersToolDefinition(EduStudentBindableUsersTool handler) {
        return definition("edu.student.bindableUsers", "查询可绑定的学生用户账号", PermCodeConst.Edu.Student.LIST, false, handler);
    }

    @Bean
    public AiToolDefinition eduCourseSearchToolDefinition(EduCourseSearchTool handler) {
        return definition("edu.course.search", "查询 EDU 课程", PermCodeConst.Edu.Course.LIST, false, handler);
    }

    @Bean
    public AiToolDefinition eduSelectionByStudentToolDefinition(EduSelectionByStudentTool handler) {
        return definition("edu.selection.byStudent", "按学生查询选课记录", PermCodeConst.Edu.Course.LIST, false, handler);
    }

    @Bean
    public AiToolDefinition eduStudentBindUserToolDefinition(EduStudentBindUserTool handler) {
        return definition("edu.student.bindUser", "绑定学生档案到用户", PermCodeConst.Edu.Student.EDIT, true, handler);
    }

    @Bean
    public AiToolDefinition eduStudentUnbindUserToolDefinition(EduStudentUnbindUserTool handler) {
        return definition("edu.student.unbindUser", "解除学生档案用户绑定", PermCodeConst.Edu.Student.EDIT, true, handler);
    }

    @Bean
    public AiToolDefinition eduCourseUpdateCapacityToolDefinition(EduCourseUpdateCapacityTool handler) {
        return definition("edu.course.updateCapacity", "修改课程容量", PermCodeConst.Edu.Course.EDIT, true, handler);
    }

    @Bean
    public AiToolDefinition eduCourseUpdateSelectionTimeToolDefinition(EduCourseUpdateSelectionTimeTool handler) {
        return definition("edu.course.updateSelectionTime", "修改课程选课时间", PermCodeConst.Edu.Course.EDIT, true, handler);
    }

    @Bean
    public AiToolDefinition eduCourseUpdateStatusToolDefinition(EduCourseUpdateStatusTool handler) {
        return definition("edu.course.updateStatus", "修改课程状态", PermCodeConst.Edu.Course.EDIT, true, handler);
    }

    @Bean
    public AiToolDefinition eduSelectionDropForStudentToolDefinition(EduSelectionDropForStudentTool handler) {
        return definition("edu.selection.dropForStudent", "为学生退选课程", PermCodeConst.Edu.SELECTION_DROP, true, handler);
    }

    private AiToolDefinition definition(String name, String description, String permissionCode, boolean mutation, AiToolHandler handler) {
        return AiToolDefinition.builder()
                .name(name)
                .description(description)
                .permissionCode(permissionCode)
                .mutation(mutation)
                .parameterClass(java.util.Map.class)
                .handler(handler)
                .build();
    }
}
