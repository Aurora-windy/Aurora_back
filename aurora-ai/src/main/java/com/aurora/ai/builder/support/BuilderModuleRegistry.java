package com.aurora.ai.builder.support;

import com.aurora.ai.builder.model.resp.BuilderModuleInfoResp;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class BuilderModuleRegistry {

    private final List<BuilderModuleInfoResp> modules = List.of(
            BuilderModuleInfoResp.builder()
                    .id("rbac")
                    .name("通用 RBAC 权限基座")
                    .category("base")
                    .description("提供登录认证、用户、角色、菜单和权限控制，是所有生成系统的底层基座。")
                    .dependencies(List.of())
                    .features(List.of("登录认证", "用户管理", "角色管理", "菜单权限", "接口权限"))
                    .status("ready")
                    .required(Boolean.TRUE)
                    .build(),
            BuilderModuleInfoResp.builder()
                    .id("hr")
                    .name("企业人员管理系统")
                    .category("business")
                    .description("覆盖部门、岗位、员工档案和考勤等企业人事管理课设高频功能。")
                    .dependencies(List.of("rbac"))
                    .features(List.of("部门管理", "岗位管理", "员工管理", "考勤管理"))
                    .status("ready")
                    .required(Boolean.FALSE)
                    .build(),
            BuilderModuleInfoResp.builder()
                    .id("edu")
                    .name("学生选课教务系统")
                    .category("business")
                    .description("覆盖学生、教师、课程、选课、成绩和课表等教务管理功能。")
                    .dependencies(List.of("rbac"))
                    .features(List.of("学生管理", "教师管理", "课程管理", "在线选课", "成绩课表"))
                    .status("ready")
                    .required(Boolean.FALSE)
                    .build()
    );

    private final Map<String, BuilderModuleInfoResp> moduleMap = modules.stream()
            .collect(Collectors.toUnmodifiableMap(BuilderModuleInfoResp::getId, Function.identity()));

    public List<BuilderModuleInfoResp> listModules() {
        return new ArrayList<>(modules);
    }

    public Optional<BuilderModuleInfoResp> findById(String id) {
        return Optional.ofNullable(moduleMap.get(id));
    }

    public Optional<ModuleDoc> findDocById(String id) {
        return findById(id).map(module -> new ModuleDoc(
                module.getId(),
                module.getName(),
                module.getCategory(),
                module.getDescription(),
                module.getDependencies(),
                module.getFeatures(),
                Boolean.TRUE.equals(module.getRequired())
        ));
    }

    public static final class ModuleDoc {
        private final String id;
        private final String name;
        private final String category;
        private final String description;
        private final List<String> dependencies;
        private final List<String> features;
        private final boolean required;

        private ModuleDoc(
                String id,
                String name,
                String category,
                String description,
                List<String> dependencies,
                List<String> features,
                boolean required
        ) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.description = description;
            this.dependencies = dependencies;
            this.features = features;
            this.required = required;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getCategory() {
            return category;
        }

        public String getDescription() {
            return description;
        }

        public List<String> getDependencies() {
            return dependencies;
        }

        public List<String> getFeatures() {
            return features;
        }

        public boolean isRequired() {
            return required;
        }
    }
}
