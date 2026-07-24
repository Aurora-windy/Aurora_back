package com.aurora.mall.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aurora.common.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_order")
public class MallOrderDO extends BaseDO {
    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    private Integer status;
    private Integer payType;
    private LocalDateTime payTime;
    private LocalDateTime shipTime;
    private LocalDateTime completeTime;
    private LocalDateTime cancelTime;
    private String cancelReason;
    private Integer isSeckill;
    private Long activityId;
}
