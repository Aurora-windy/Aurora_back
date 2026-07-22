package com.aurora.mall.product.model.resp;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProductResp {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private Integer status;
    private LocalDateTime createTime;
}
