package com.aurora.ai.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 对话角色枚举（双值：int code + string value）
 *
 * <p>spec: docs/specs/2026-07-05-global-variables.md §4.1.7</p>
 * <p>ai_chat_message.role 字段存 int；调 LLM 时用 {@link #value}（"user"/"assistant"/"system"，与 OpenAI/Spring AI 协议对齐）。</p>
 * <p>{@code @JsonValue} 标在 {@link #value} 上，序列化为字符串。</p>
 */
@Getter
@AllArgsConstructor
@Schema(description = "对话角色：1=user 2=assistant 3=system")
public enum ChatRoleEnum {

    USER      (1, "user",      "用户"),
    ASSISTANT (2, "assistant", "助手"),
    SYSTEM    (3, "system",    "系统");

    @Schema(description = "角色编码（DB 存储）")
    private final int code;

    @JsonValue
    @Schema(description = "角色标识（LLM 协议）")
    private final String value;

    @Schema(description = "中文描述")
    private final String label;

    public static ChatRoleEnum fromCode(int code) {
        for (ChatRoleEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }

    public static ChatRoleEnum fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (ChatRoleEnum e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }
        return null;
    }
}
