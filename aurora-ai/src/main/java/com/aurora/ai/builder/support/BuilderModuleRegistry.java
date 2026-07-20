package com.aurora.ai.builder.support;

import com.aurora.ai.builder.model.resp.BuilderModuleResp;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class BuilderModuleRegistry {

    private final List<BuilderModuleResp> modules = List.of(
            BuilderModuleResp.builder()
                    .id("rbac")
                    .name("通用 RBAC 权限基座")
                    .category("base")
                    .description("提供登录认证、用户、角色、菜单和权限控制，是所有生成系统的底层基座。")
                    .dependencies(List.of())
                    .features(List.of("登录认证", "用户管理", "角色管理", "菜单权限", "接口权限"))
                    .status("ready")
                    .required(Boolean.TRUE)
                    .build(),
            BuilderModuleResp.builder()
                    .id("hr")
                    .name("企业人员管理系统")
                    .category("business")
                    .description("覆盖部门、岗位、员工档案和考勤等企业人事管理课设高频功能。")
                    .dependencies(List.of("rbac"))
                    .features(List.of("部门管理", "岗位管理", "员工管理", "考勤管理"))
                    .status("ready")
                    .required(Boolean.FALSE)
                    .build(),
            BuilderModuleResp.builder()
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

    private final Map<String, BuilderModuleResp> moduleMap = modules.stream()
            .collect(Collectors.toUnmodifiableMap(BuilderModuleResp::getId, Function.identity()));

    public List<BuilderModuleResp> listModules() {
        return new ArrayList<>(modules);
    }

    public Optional<BuilderModuleResp> findById(String id) {
        return Optional.ofNullable(moduleMap.get(id));
    }
}