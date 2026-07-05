package com.aurora.oj.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 编程语言枚举（双值：int code + string value）
 *
 * <p>spec: docs/specs/2026-07-05-global-variables.md §4.1.5 / §4.1.5.1</p>
 * <p>oj_submission.language 字段存 int；接口响应用 {@link #value}（"java" 等），方便前端 Monaco Editor 切语言。</p>
 * <p>{@code @JsonValue} 标在 {@link #value} 上，序列化为字符串。</p>
 */
@Getter
@AllArgsConstructor
@Schema(description = "编程语言：1=java 2=python 3=go 4=cpp")
public enum LanguageEnum {

    JAVA   (1, "java",   "Java"),
    PYTHON (2, "python", "Python"),
    GO     (3, "go",     "Go"),
    CPP    (4, "cpp",    "C++");

    @Schema(description = "语言编码（DB 存储）")
    private final int code;

    @JsonValue
    @Schema(description = "语言标识（接口响应/前端使用）")
    private final String value;

    @Schema(description = "显示名称")
    private final String label;

    public static LanguageEnum fromCode(int code) {
        for (LanguageEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }

    public static LanguageEnum fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (LanguageEnum e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }
        return null;
    }
}
