package com.aurora.mall.seckill.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aurora.common.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_seckill_activity")
public class MallSeckillActivityDO extends BaseDO {
    private Long productId;
    private BigDecimal seckillPrice;
    private Integer seckillStock;
    private Integer availableStock;
    private Integer limitPerUser;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
}
