package com.aurora.mall.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aurora.common.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_order")
public class MallOrderDO extends BaseDO {
    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    private Integer status;
}
