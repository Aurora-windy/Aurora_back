package com.aurora.mall.seckill.model.req;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillActivitySaveReq {
    @NotNull private Long productId;
    @NotNull @DecimalMin("0.01") private BigDecimal seckillPrice;
    @NotNull @Min(1) private Integer seckillStock;
    @Min(1) private Integer limitPerUser = 1;
    @NotNull private LocalDateTime startTime;
    @NotNull private LocalDateTime endTime;
}
