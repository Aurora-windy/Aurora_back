package com.aurora.ai.builder.support;

import com.aurora.ai.builder.model.resp.BuilderModuleInfoResp;
import com.aurora.ai.builder.model.resp.BuilderPlanResp;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class BuilderRequirementParser {

    private final BuilderModuleRegistry moduleRegistry;

    public BuilderRequirementParser(BuilderModuleRegistry moduleRegistry) {
        this.moduleRegistry = moduleRegistry;
    }

    public BuilderPlanResp parse(String requirement) {
        String normalizedRequirement = StringUtils.hasText(requirement) ? requirement.trim() : "";
        Set<String> excludedModuleIds = detectExcludedModules(normalizedRequirement);
        LinkedHashSet<String> selectedModuleIds = new LinkedHashSet<>();
        selectedModuleIds.add("rbac");

        if (containsAny(normalizedRequirement, "人事", "人力", "人员", "员工", "部门", "岗位", "考勤", "hr", "HR")) {
            selectedModuleIds.add("hr");
        }
        if (containsAny(normalizedRequirement, "教务", "学生", "教师", "课程", "选课", "成绩", "课表", "edu", "EDU")) {
            selectedModuleIds.add("edu");
        }

        selectedModuleIds.removeAll(excludedModuleIds);
        selectedModuleIds.add("rbac");

        List<String> missingDependencies = findMissingDependencies(selectedModuleIds);
        List<String> warnings = new ArrayList<>();
        if (selectedModuleIds.size() == 1) {
            warnings.add("暂未识别到明确业务模块，仅保留 RBAC 基座；建议补充人事、教务等业务关键词。");
        }
        if (!missingDependencies.isEmpty()) {
            warnings.add("存在缺失依赖，请先补齐依赖模块再生成项目。");
        }

        return BuilderPlanResp.builder()
                .requestId(UUID.randomUUID().toString())
                .requirement(normalizedRequirement)
                .selectedModules(toModules(selectedModuleIds))
                .excludedModules(new ArrayList<>(excludedModuleIds))
                .missingDependencies(missingDependencies)
                .warnings(warnings)
                .nextStep(missingDependencies.isEmpty() ? "preview" : "fix_dependencies")
                .build();
    }

    private Set<String> detectExcludedModules(String requirement) {
        LinkedHashSet<String> excluded = new LinkedHashSet<>();
        if (containsAny(requirement, "不要选课", "不需要选课", "不要教务", "不做教务", "排除教务", "不要学生", "不要课程")) {
            excluded.add("edu");
        }
        if (containsAny(requirement, "不要人事", "不需要人事", "不要员工", "不做员工", "不要考勤", "排除人事")) {
            excluded.add("hr");
        }
        return excluded;
    }

    private List<String> findMissingDependencies(Set<String> selectedModuleIds) {
        List<String> missingDependencies = new ArrayList<>();
        for (String moduleId : selectedModuleIds) {
            moduleRegistry.findById(moduleId).ifPresent(module -> module.getDependencies().forEach(dependency -> {
                if (!selectedModuleIds.contains(dependency) && !missingDependencies.contains(dependency)) {
                    missingDependencies.add(dependency);
                }
            }));
        }
        return missingDependencies;
    }

    private List<BuilderModuleInfoResp> toModules(Set<String> moduleIds) {
        List<BuilderModuleInfoResp> modules = new ArrayList<>();
        for (String moduleId : moduleIds) {
            moduleRegistry.findById(moduleId).ifPresent(modules::add);
        }
        return modules;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
