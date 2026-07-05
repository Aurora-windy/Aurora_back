package com.aurora.mall.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 库存变动溯源业务类型枚举
 *
 * <p>spec: docs/specs/2026-07-05-global-variables.md §4.1.6</p>
 * <p>mall_stock_log.biz_type 字段；每次库存变动（含正负）必带此值，便于事后审计与异常排查。</p>
 */
@Getter
@AllArgsConstructor
@Schema(description = "库存业务类型：1下单扣减 2取消回滚 3退款回滚 4人工调整")
public enum StockBizTypeEnum {

    ORDER_DEDUCT   (1, "下单扣减"),
    CANCEL_ROLLBACK(2, "取消回滚"),
    REFUND_ROLLBACK(3, "退款回滚"),
    MANUAL_ADJUST  (4, "人工调整");

    @JsonValue
    @Schema(description = "状态码")
    private final int code;

    @Schema(description = "描述")
    private final String label;

    public static StockBizTypeEnum fromCode(int code) {
        for (StockBizTypeEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }
}
