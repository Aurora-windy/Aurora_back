package com.aurora.mall.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付方式枚举
 *
 * <p>spec: docs/specs/2026-07-05-global-variables.md §4.1.6</p>
 * <p>mall_order.pay_type 字段；本期仅 SIMULATION 实装，微信/支付宝占位（毕设答辩演示用模拟支付）。</p>
 */
@Getter
@AllArgsConstructor
@Schema(description = "支付方式：1微信 2支付宝 3模拟支付")
public enum PayTypeEnum {

    WECHAT     (1, "微信"),
    ALIPAY     (2, "支付宝"),
    SIMULATION (3, "模拟支付");

    @JsonValue
    @Schema(description = "状态码")
    private final int code;

    @Schema(description = "描述")
    private final String label;

    public static PayTypeEnum fromCode(int code) {
        for (PayTypeEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }
}
