package com.aurora.mall.cart.model.resp;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CartResp {
    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal price;
    private Integer stock;
    private Integer productStatus;
    private Integer quantity;
    private BigDecimal subtotalAmount;
}
