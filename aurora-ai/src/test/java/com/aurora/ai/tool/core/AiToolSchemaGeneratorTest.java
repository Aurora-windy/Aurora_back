package com.aurora.ai.tool.core;

import com.aurora.ai.tool.core.AiToolDefinition.ParamField;
import com.aurora.ai.tool.core.AiToolDefinition.ParamType;
import com.aurora.ai.tool.edu.EduAiToolConfig;
import com.aurora.ai.tool.edu.EduCourseSearchTool;
import com.aurora.ai.tool.edu.EduCourseUpdateCapacityTool;
import com.aurora.ai.tool.edu.EduCourseUpdateSelectionTimeTool;
import com.aurora.ai.tool.edu.EduCourseUpdateStatusTool;
import com.aurora.ai.tool.edu.EduSelectionByStudentTool;
import com.aurora.ai.tool.edu.EduSelectionDropForStudentTool;
import com.aurora.ai.tool.edu.EduStudentBindableUsersTool;
import com.aurora.ai.tool.edu.EduStudentBindUserTool;
import com.aurora.ai.tool.edu.EduStudentSearchTool;
import com.aurora.ai.tool.edu.EduStudentUnbindUserTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * T2 验收：10 个 EDU 工具的 FC schema 生成（真实 EduAiToolConfig 声明，非复制品）。
 */
class AiToolSchemaGeneratorTest {

    private final AiToolSchemaGenerator generator = new AiToolSchemaGenerator(new ObjectMapper());
    private final EduAiToolConfig config = new EduAiToolConfig();

    private List<AiToolDefinition> allTenDefinitions() {
        return List.of(
                config.eduStudentSearchToolDefinition(mock(EduStudentSearchTool.class)),
                config.eduStudentBindableUsersToolDefinition(mock(EduStudentBindableUsersTool.class)),
                config.eduCourseSearchToolDefinition(mock(EduCourseSearchTool.class)),
                config.eduSelectionByStudentToolDefinition(mock(EduSelectionByStudentTool.class)),
                config.eduStudentBindUserToolDefinition(mock(EduStudentBindUserTool.class)),
                config.eduStudentUnbindUserToolDefinition(mock(EduStudentUnbindUserTool.class)),
                config.eduCourseUpdateCapacityToolDefinition(mock(EduCourseUpdateCapacityTool.class)),
                config.eduCourseUpdateSelectionTimeToolDefinition(mock(EduCourseUpdateSelectionTimeTool.class)),
                config.eduCourseUpdateStatusToolDefinition(mock(EduCourseUpdateStatusTool.class)),
                config.eduSelectionDropForStudentToolDefinition(mock(EduSelectionDropForStudentTool.class)));
    }

    @Test
    void allTenTools_generateValidFunctionEntry() {
        List<AiToolDefinition> definitions = allTenDefinitions();
        assertThat(definitions).hasSize(10);
        for (AiToolDefinition definition : definitions) {
            Map<String, Object> entry = generator.generate(definition);
            assertThat(entry.get("type")).as("%s type", definition.getName()).isEqualTo("function");
            Map<String, Object> function = functionOf(entry, definition.getName());
            assertThat(function.get("name")).isEqualTo(AiToolSchemaGenerator.toWireName(definition.getName()));
            // OpenAI 兼容 API 约束：function.name 只允许 [a-zA-Z0-9_-]+（DeepSeek 官方实测拒绝点号）
            assertThat((String) function.get("name")).matches("^[a-zA-Z0-9_-]+$");
            assertThat((String) function.get("description")).as("%s description", definition.getName()).isNotBlank();
            Map<String, Object> parameters = parametersOf(entry, definition.getName());
            assertThat(parameters.get("type")).isEqualTo("object");
            assertThat(parameters.get("properties")).isInstanceOf(Map.class);
            assertThat(parameters.get("required")).isInstanceOf(List.class);
        }
    }

    @Test
    void wireName_roundTrip_restoresRegistryName() {
        for (AiToolDefinition definition : allTenDefinitions()) {
            String wire = AiToolSchemaGenerator.toWireName(definition.getName());
            assertThat(wire).as("%s wire", definition.getName()).doesNotContain(".");
            assertThat(AiToolSchemaGenerator.fromWireName(wire)).isEqualTo(definition.getName());
        }
    }

    @Test
    void everyParamField_hasTypeAndDescription() {
        for (AiToolDefinition definition : allTenDefinitions()) {
            Map<String, Object> properties = propertiesOf(generator.generate(definition), definition.getName());
            for (ParamField field : definition.getParamSchema()) {
                Map<String, Object> property = propertyOf(properties, field.name(), definition.getName());
                assertThat(property.get("type")).as("%s.%s type", definition.getName(), field.name()).isNotNull();
                assertThat((String) property.get("description"))
                        .as("%s.%s description", definition.getName(), field.name()).isNotBlank();
                assertThat(property).as("%s.%s 只含 type/format/description", definition.getName(), field.name())
                        .containsOnlyKeys("type", "format", "description");
            }
        }
    }

    @Test
    void requiredFields_matchDeclaration() {
        Map<String, Object> properties = propertiesOf(
                generator.generate(byName("edu.selection.byStudent")), "edu.selection.byStudent");
        assertThat(properties).containsOnlyKeys("studentId");
        assertThat(requiredOf(generator.generate(byName("edu.selection.byStudent")), "edu.selection.byStudent"))
                .containsExactly("studentId");

        assertThat(requiredOf(generator.generate(byName("edu.student.search")), "edu.student.search")).isEmpty();
        assertThat(requiredOf(generator.generate(byName("edu.course.search")), "edu.course.search")).isEmpty();
        assertThat(requiredOf(generator.generate(byName("edu.course.updateCapacity")), "edu.course.updateCapacity"))
                .containsExactlyInAnyOrder("courseId", "capacity");
    }

    @Test
    void datetimeParams_mapToStringWithDateFormat() {
        Map<String, Object> properties = propertiesOf(
                generator.generate(byName("edu.course.updateSelectionTime")), "edu.course.updateSelectionTime");
        for (String field : List.of("startTime", "endTime")) {
            Map<String, Object> property = propertyOf(properties, field, "edu.course.updateSelectionTime");
            assertThat(property.get("type")).isEqualTo("string");
            assertThat(property.get("format")).isEqualTo("date-time");
        }
    }

    @Test
    void longType_mapsToInteger() {
        AiToolDefinition definition = AiToolDefinition.builder()
                .name("t.long").description("d")
                .paramSchema(List.of(new ParamField("id", ParamType.LONG, true, "长整型 ID")))
                .build();
        Map<String, Object> properties = propertiesOf(generator.generate(definition), "t.long");
        assertThat(propertyOf(properties, "id", "t.long").get("type")).isEqualTo("integer");
    }

    @Test
    void mutationTools_haveRiskNote_queryToolsDoNot() {
        for (AiToolDefinition definition : allTenDefinitions()) {
            if (Boolean.TRUE.equals(definition.getMutation())) {
                assertThat(definition.getRiskNote()).as("%s riskNote", definition.getName()).isNotBlank();
            } else {
                assertThat(definition.getRiskNote()).as("%s riskNote", definition.getName()).isNullOrEmpty();
            }
        }
    }

    @Test
    void toolsSchemaJson_overridesParamSchema() {
        AiToolDefinition definition = AiToolDefinition.builder()
                .name("mcp_demo_get").description("MCP 远程工具")
                .paramSchema(List.of(new ParamField("ignored", ParamType.STRING, false, "应被忽略")))
                .toolsSchemaJson("{\"type\":\"object\",\"properties\":{\"q\":{\"type\":\"string\"}},\"required\":[\"q\"]}")
                .build();
        Map<String, Object> parameters = parametersOf(generator.generate(definition), "mcp_demo_get");
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) parameters.get("properties");
        assertThat(properties).containsOnlyKeys("q");
        assertThat((List<String>) parameters.get("required")).containsExactly("q");
    }

    @Test
    void invalidToolsSchemaJson_fallsBackToEmptyObject() {
        AiToolDefinition definition = AiToolDefinition.builder()
                .name("mcp_bad").description("坏 schema")
                .toolsSchemaJson("{not-json")
                .build();
        Map<String, Object> parameters = parametersOf(generator.generate(definition), "mcp_bad");
        assertThat(parameters.get("type")).isEqualTo("object");
        assertThat((Map<?, ?>) parameters.get("properties")).isEmpty();
    }

    private AiToolDefinition byName(String name) {
        return allTenDefinitions().stream()
                .filter(d -> name.equals(d.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("未声明工具: " + name));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> functionOf(Map<String, Object> entry, String toolName) {
        return (Map<String, Object>) requireEntry(entry, toolName).get("function");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parametersOf(Map<String, Object> entry, String toolName) {
        return (Map<String, Object>) functionOf(entry, toolName).get("parameters");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> propertiesOf(Map<String, Object> entry, String toolName) {
        return (Map<String, Object>) parametersOf(entry, toolName).get("properties");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> propertyOf(Map<String, Object> properties, String field, String toolName) {
        Map<String, Object> property = (Map<String, Object>) properties.get(field);
        assertThat(property).as("%s.%s property", toolName, field).isNotNull();
        return property;
    }

    @SuppressWarnings("unchecked")
    private List<String> requiredOf(Map<String, Object> entry, String toolName) {
        return (List<String>) parametersOf(entry, toolName).get("required");
    }

    private Map<String, Object> requireEntry(Map<String, Object> entry, String toolName) {
        assertThat(entry).as("%s entry", toolName).isNotNull();
        return entry;
    }
}
