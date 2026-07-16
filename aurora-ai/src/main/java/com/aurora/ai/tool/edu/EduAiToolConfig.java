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
        return definition("edu.student.search", "Search EDU student profiles", PermCodeConst.Edu.Student.LIST, false, handler);
    }

    @Bean
    public AiToolDefinition eduStudentBindableUsersToolDefinition(EduStudentBindableUsersTool handler) {
        return definition("edu.student.bindableUsers", "Search bindable student user accounts", PermCodeConst.Edu.Student.LIST, false, handler);
    }

    @Bean
    public AiToolDefinition eduCourseSearchToolDefinition(EduCourseSearchTool handler) {
        return definition("edu.course.search", "Search EDU courses", PermCodeConst.Edu.Course.LIST, false, handler);
    }

    @Bean
    public AiToolDefinition eduSelectionByStudentToolDefinition(EduSelectionByStudentTool handler) {
        return definition("edu.selection.byStudent", "List selections by student", PermCodeConst.Edu.Course.LIST, false, handler);
    }

    @Bean
    public AiToolDefinition eduStudentBindUserToolDefinition(EduStudentBindUserTool handler) {
        return definition("edu.student.bindUser", "Bind student profile to user", PermCodeConst.Edu.Student.EDIT, true, handler);
    }

    @Bean
    public AiToolDefinition eduStudentUnbindUserToolDefinition(EduStudentUnbindUserTool handler) {
        return definition("edu.student.unbindUser", "Unbind student profile user", PermCodeConst.Edu.Student.EDIT, true, handler);
    }

    @Bean
    public AiToolDefinition eduCourseUpdateCapacityToolDefinition(EduCourseUpdateCapacityTool handler) {
        return definition("edu.course.updateCapacity", "Update course capacity", PermCodeConst.Edu.Course.EDIT, true, handler);
    }

    @Bean
    public AiToolDefinition eduCourseUpdateSelectionTimeToolDefinition(EduCourseUpdateSelectionTimeTool handler) {
        return definition("edu.course.updateSelectionTime", "Update course selection time", PermCodeConst.Edu.Course.EDIT, true, handler);
    }

    @Bean
    public AiToolDefinition eduCourseUpdateStatusToolDefinition(EduCourseUpdateStatusTool handler) {
        return definition("edu.course.updateStatus", "Update course status", PermCodeConst.Edu.Course.EDIT, true, handler);
    }

    @Bean
    public AiToolDefinition eduSelectionDropForStudentToolDefinition(EduSelectionDropForStudentTool handler) {
        return definition("edu.selection.dropForStudent", "Drop course for student", PermCodeConst.Edu.SELECTION_DROP, true, handler);
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