package com.aurora.mall.seckill.model.resp;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class SeckillActivityResp {
    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal seckillPrice;
    private BigDecimal originalPrice;
    private Integer seckillStock;
    private Integer availableStock;
    private Integer limitPerUser;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private LocalDateTime createTime;
}
