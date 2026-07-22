package com.aurora.ai.builder.support;

import com.aurora.ai.builder.model.resp.BuilderGeneratePreviewResp;
import com.aurora.common.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BuilderPreviewGeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    void generate_shouldAddRbacAndCreatePreviewFiles() {
        BuilderPreviewGenerator generator = new BuilderPreviewGenerator(new BuilderModuleRegistry(), tempDir);

        BuilderGeneratePreviewResp resp = generator.generate("我要企业员工考勤管理系统", List.of("hr"));

        assertThat(resp.getSelectedModules()).containsExactly("rbac", "hr");
        assertThat(resp.getGeneratedFiles()).extracting("path")
                .containsExactly(
                        "README.md",
                        "docs/SRS.md",
                        "backend/MODULES.md",
                        "frontend/MODULES.md",
                        "database/changelog/MODULES.md"
                );
        assertThat(Files.exists(tempDir.resolve(resp.getBuildId()).resolve("README.md"))).isTrue();
        assertThat(Files.exists(tempDir.resolve(resp.getBuildId()).resolve("docs/SRS.md"))).isTrue();
    }

    @Test
    void generate_shouldRejectUnsupportedModule() {
        BuilderPreviewGenerator generator = new BuilderPreviewGenerator(new BuilderModuleRegistry(), tempDir);

        assertThatThrownBy(() -> generator.generate("我要生成未知模块", List.of("mall")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("unsupported builder module");
    }

    @Test
    void generate_shouldNotLeakAbsolutePath() {
        BuilderPreviewGenerator generator = new BuilderPreviewGenerator(new BuilderModuleRegistry(), tempDir);

        BuilderGeneratePreviewResp resp = generator.generate("我要学生选课系统", List.of("edu"));

        assertThat(resp.getGeneratedFiles())
                .allSatisfy(file -> assertThat(file.getPath()).doesNotContain(tempDir.toString()).doesNotContain(":"));
    }
}
