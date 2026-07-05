package com.aurora.oj.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 题目收藏类型枚举
 *
 * <p>spec: docs/specs/2026-07-05-global-variables.md §4.1.5</p>
 * <p>oj_favorite.type 字段；同一道题同一用户只能有一条记录，type 区分语义。</p>
 */
@Getter
@AllArgsConstructor
@Schema(description = "收藏类型：1错题 2收藏")
public enum FavoriteTypeEnum {

    WRONG     (1, "错题"),
    FAVORITE  (2, "收藏");

    @JsonValue
    @Schema(description = "状态码")
    private final int code;

    @Schema(description = "描述")
    private final String label;

    public static FavoriteTypeEnum fromCode(int code) {
        for (FavoriteTypeEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }
}
