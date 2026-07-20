package com.aurora.ai.builder.support;

import com.aurora.ai.builder.model.resp.BuilderPlanResp;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BuilderRequirementParserTest {

    private final BuilderRequirementParser parser = new BuilderRequirementParser(new BuilderModuleRegistry());

    @Test
    void parse_shouldSelectRbacAndHrForHumanResourceRequirement() {
        BuilderPlanResp plan = parser.parse("我需要做企业人事管理课设，包含部门、员工、考勤");

        assertThat(plan.getSelectedModules()).extracting("id").containsExactly("rbac", "hr");
        assertThat(plan.getExcludedModules()).isEmpty();
        assertThat(plan.getMissingDependencies()).isEmpty();
        assertThat(plan.getNextStep()).isEqualTo("preview");
    }

    @Test
    void parse_shouldRespectEduExclusion() {
        BuilderPlanResp plan = parser.parse("我需要企业人事管理系统，包含部门、员工、考勤，不要选课系统");

        assertThat(plan.getSelectedModules()).extracting("id").containsExactly("rbac", "hr");
        assertThat(plan.getExcludedModules()).contains("edu");
        assertThat(plan.getSelectedModules()).extracting("id").doesNotContain("edu");
    }

    @Test
    void parse_shouldSelectEduForCourseSelectionRequirement() {
        BuilderPlanResp plan = parser.parse("我要学生选课教务系统，包含课程、教师、成绩和课表");

        assertThat(plan.getSelectedModules()).extracting("id").containsExactly("rbac", "edu");
        assertThat(plan.getMissingDependencies()).isEmpty();
    }
}