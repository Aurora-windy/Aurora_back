package com.aurora.mall.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 商品上下架状态枚举
 *
 * <p>spec: docs/specs/2026-07-05-global-variables.md §4.1.6</p>
 * <p>mall_product.status 字段。语义等同于 {@code StatusEnum}，但 MALL 域习惯用「上架/下架」文案，单独建枚举。</p>
 */
@Getter
@AllArgsConstructor
@Schema(description = "商品状态：0下架 1上架")
public enum ProductStatusEnum {

    OFF_SHELF (0, "下架"),
    ON_SHELF  (1, "上架");

    @JsonValue
    @Schema(description = "状态码")
    private final int code;

    @Schema(description = "描述")
    private final String label;

    public static ProductStatusEnum fromCode(int code) {
        for (ProductStatusEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }
}
