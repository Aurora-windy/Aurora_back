package com.aurora.ai.builder.support;

import com.aurora.ai.builder.model.resp.BuilderGeneratePreviewResp;
import com.aurora.ai.builder.model.resp.BuilderGeneratedFileResp;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class BuilderPreviewGenerator {

    private final BuilderModuleRegistry moduleRegistry;
    private final Path outputRoot;

    @Autowired
    public BuilderPreviewGenerator(BuilderModuleRegistry moduleRegistry) {
        this(moduleRegistry, Path.of(System.getProperty("user.dir"), "builder-output", "preview"));
    }

    BuilderPreviewGenerator(BuilderModuleRegistry moduleRegistry, Path outputRoot) {
        this.moduleRegistry = moduleRegistry;
        this.outputRoot = outputRoot.toAbsolutePath().normalize();
    }

    public BuilderGeneratePreviewResp generate(String rawRequirement, List<String> moduleIds) {
        String requirement = StringUtils.hasText(rawRequirement) ? rawRequirement.trim() : "";
        LinkedHashSet<String> selectedModuleIds = normalizeModuleIds(moduleIds);
        List<BuilderModuleRegistry.ModuleDoc> selectedModules = toModules(selectedModuleIds);
        validateDependencies(selectedModuleIds, selectedModules);

        String buildId = UUID.randomUUID().toString();
        Path buildDir = outputRoot.resolve(buildId).normalize();
        if (!buildDir.startsWith(outputRoot)) {
            throw new BizException(BizCode.PARAM_ERROR, "invalid builder output path");
        }

        List<BuilderGeneratedFileResp> generatedFiles = writePreviewFiles(buildDir, requirement, selectedModules);
        List<String> warnings = new ArrayList<>();
        warnings.add("当前产物为生成预览骨架，不是完整可运行项目；ZIP 与真实模板拼装将在后续批次完成。");
        if (selectedModules.size() == 1) {
            warnings.add("当前仅包含 RBAC 基座，建议补充 HR 或 EDU 业务模块后再生成正式项目。");
        }

        return BuilderGeneratePreviewResp.builder()
                .buildId(buildId)
                .requirement(requirement)
                .selectedModules(new ArrayList<>(selectedModuleIds))
                .generatedFiles(generatedFiles)
                .warnings(warnings)
                .nextStep("template_zip")
                .build();
    }

    private LinkedHashSet<String> normalizeModuleIds(List<String> moduleIds) {
        LinkedHashSet<String> selectedModuleIds = new LinkedHashSet<>();
        selectedModuleIds.add("rbac");
        if (moduleIds == null) {
            return selectedModuleIds;
        }
        for (String moduleId : moduleIds) {
            if (!StringUtils.hasText(moduleId)) {
                continue;
            }
            String normalizedModuleId = moduleId.trim();
            if (moduleRegistry.findDocById(normalizedModuleId).isEmpty()) {
                throw new BizException(BizCode.PARAM_ERROR, "unsupported builder module: " + normalizedModuleId);
            }
            selectedModuleIds.add(normalizedModuleId);
        }
        return selectedModuleIds;
    }

    private List<BuilderModuleRegistry.ModuleDoc> toModules(Set<String> moduleIds) {
        List<BuilderModuleRegistry.ModuleDoc> modules = new ArrayList<>();
        for (String moduleId : moduleIds) {
            moduleRegistry.findDocById(moduleId).ifPresent(modules::add);
        }
        return modules;
    }

    private void validateDependencies(Set<String> selectedModuleIds, List<BuilderModuleRegistry.ModuleDoc> modules) {
        for (BuilderModuleRegistry.ModuleDoc module : modules) {
            for (String dependency : module.getDependencies()) {
                if (!selectedModuleIds.contains(dependency)) {
                    throw new BizException(BizCode.PARAM_ERROR, "missing builder module dependency: " + dependency);
                }
            }
        }
    }

    private List<BuilderGeneratedFileResp> writePreviewFiles(
            Path buildDir,
            String requirement,
            List<BuilderModuleRegistry.ModuleDoc> selectedModules
    ) {
        try {
            Files.createDirectories(buildDir);
            List<BuilderGeneratedFileResp> files = new ArrayList<>();
            files.add(writeFile(buildDir, "README.md", "document", "项目说明", renderReadme(requirement, selectedModules)));
            files.add(writeFile(buildDir, "docs/SRS.md", "document", "需求规格说明", renderSrs(requirement, selectedModules)));
            files.add(writeFile(buildDir, "backend/MODULES.md", "backend", "后端模块清单", renderModuleManifest("后端模块清单", selectedModules)));
            files.add(writeFile(buildDir, "frontend/MODULES.md", "frontend", "前端模块清单", renderModuleManifest("前端模块清单", selectedModules)));
            files.add(writeFile(buildDir, "database/changelog/MODULES.md", "database", "数据库迁移清单", renderModuleManifest("数据库迁移清单", selectedModules)));
            return files;
        } catch (IOException ex) {
            throw new UncheckedIOException("builder preview generation failed", ex);
        }
    }

    private BuilderGeneratedFileResp writeFile(
            Path buildDir,
            String relativePath,
            String type,
            String description,
            String content
    ) throws IOException {
        Path file = buildDir.resolve(relativePath).normalize();
        if (!file.startsWith(buildDir)) {
            throw new BizException(BizCode.PARAM_ERROR, "invalid builder file path");
        }
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return BuilderGeneratedFileResp.builder()
                .path(relativePath.replace('\\', '/'))
                .type(type)
                .description(description)
                .sizeBytes(Files.size(file))
                .build();
    }

    private String renderReadme(String requirement, List<BuilderModuleRegistry.ModuleDoc> modules) {
        return """
                # AURORA Builder Preview

                ## 项目简介

                本目录由 AURORA Builder 根据用户需求生成，用于预览模块组合与后续模板拼装结构。

                用户需求：

                > %s

                ## 技术栈

                - Backend: Spring Boot / MyBatis-Plus / Sa-Token / Liquibase
                - Frontend: Vue 3 / TypeScript / Arco Design Vue
                - Database: MySQL

                ## 模块清单

                %s

                ## 默认账号

                - 管理员账号：admin
                - 默认密码：admin123

                ## 启动说明

                当前批次仅生成预览骨架。真实源码、ZIP 下载和可运行启动步骤将在后续模板拼装批次补齐。
                """.formatted(escapeMarkdown(requirement), renderModuleBullets(modules));
    }

    private String renderSrs(String requirement, List<BuilderModuleRegistry.ModuleDoc> modules) {
        return """
                # 软件需求规格说明书

                ## 项目背景

                用户希望生成一个基于 AURORA 模块库的后台管理系统。

                原始需求：

                > %s

                ## 用户角色

                - 系统管理员：管理用户、角色、菜单和权限。
                - 业务管理员：使用所选业务模块完成日常管理。

                ## 功能需求

                %s

                ## 非功能需求

                - 权限控制必须经过 RBAC 基座。
                - 接口响应保持统一 Result 结构。
                - 数据库变更必须通过 Liquibase 管理。

                ## 模块说明

                %s

                ## 验收标准

                - 生成方案中的模块必须全部来自 Builder 注册表。
                - RBAC 必须作为基础模块存在。
                - README 与 SRS 中的模块清单必须与生成请求一致。
                """.formatted(escapeMarkdown(requirement), renderFeatureBullets(modules), renderModuleDetails(modules));
    }

    private String renderModuleManifest(String title, List<BuilderModuleRegistry.ModuleDoc> modules) {
        return "# " + title + "\n\n" + renderModuleDetails(modules)
                + "\n当前文件用于固定生成预览结构，真实模板文件将在后续批次接入。\n";
    }

    private String renderModuleBullets(List<BuilderModuleRegistry.ModuleDoc> modules) {
        StringBuilder builder = new StringBuilder();
        for (BuilderModuleRegistry.ModuleDoc module : modules) {
            builder.append("- ").append(module.getName()).append(" (`").append(module.getId()).append("`): ")
                    .append(module.getDescription()).append('\n');
        }
        return builder.toString();
    }

    private String renderFeatureBullets(List<BuilderModuleRegistry.ModuleDoc> modules) {
        StringBuilder builder = new StringBuilder();
        for (BuilderModuleRegistry.ModuleDoc module : modules) {
            for (String feature : module.getFeatures()) {
                builder.append("- ").append(feature).append("：来源模块 ").append(module.getName()).append('\n');
            }
        }
        return builder.toString();
    }

    private String renderModuleDetails(List<BuilderModuleRegistry.ModuleDoc> modules) {
        StringBuilder builder = new StringBuilder();
        for (BuilderModuleRegistry.ModuleDoc module : modules) {
            builder.append("## ").append(module.getName()).append('\n')
                    .append("- 模块 ID：`").append(module.getId()).append("`\n")
                    .append("- 模块类型：").append(module.getCategory()).append('\n')
                    .append("- 是否必选：").append(module.isRequired() ? "是" : "否").append('\n')
                    .append("- 依赖模块：")
                    .append(module.getDependencies().isEmpty() ? "无" : String.join(" / ", module.getDependencies()))
                    .append("\n")
                    .append("- 核心功能：").append(String.join(" / ", module.getFeatures())).append("\n\n");
        }
        return builder.toString();
    }

    private String escapeMarkdown(String value) {
        return value.replace("\r", " ").replace("\n", " ").trim();
    }
}
