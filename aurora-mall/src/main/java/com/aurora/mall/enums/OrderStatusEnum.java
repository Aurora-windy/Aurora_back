package com.aurora.mall.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态枚举
 *
 * <p>spec: docs/specs/2026-07-05-global-variables.md §4.1.6</p>
 * <p>mall_order.status 字段。状态机：0→1（支付）→2（发货）→3（完成）；0→4（取消）；1/2→5（退款）。</p>
 * <p>扩展值预留：6=待发货（如未来拆分待发货/已发货）。</p>
 */
@Getter
@AllArgsConstructor
@Schema(description = "订单状态：0待支付 1已支付 2已发货 3已完成 4已取消 5已退款")
public enum OrderStatusEnum {

    PENDING_PAYMENT (0, "待支付"),
    PAID            (1, "已支付"),
    SHIPPED         (2, "已发货"),
    COMPLETED       (3, "已完成"),
    CANCELLED       (4, "已取消"),
    REFUNDED        (5, "已退款");

    @JsonValue
    @Schema(description = "状态码")
    private final int code;

    @Schema(description = "描述")
    private final String label;

    public static OrderStatusEnum fromCode(int code) {
        for (OrderStatusEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }
}
