package com.aurora.ai.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI 文档处理状态枚举
 *
 * <p>spec: docs/specs/2026-07-05-global-variables.md §4.1.7</p>
 * <p>ai_document.status 字段。0=处理中（向量切片/Prompt 拼装中），由异步任务回填 1/2。</p>
 */
@Getter
@AllArgsConstructor
@Schema(description = "文档状态：0处理中 1完成 2失败")
public enum DocStatusEnum {

    PROCESSING (0, "处理中"),
    COMPLETED  (1, "完成"),
    FAILED     (2, "失败");

    @JsonValue
    @Schema(description = "状态码")
    private final int code;

    @Schema(description = "描述")
    private final String label;

    public static DocStatusEnum fromCode(int code) {
        for (DocStatusEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }
}
