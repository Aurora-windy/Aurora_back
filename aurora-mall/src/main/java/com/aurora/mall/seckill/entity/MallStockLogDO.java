package com.aurora.mall.seckill.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aurora.common.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_stock_log")
public class MallStockLogDO extends BaseDO {
    private Long productId;
    private Integer bizType;
    private Integer quantity;
    private Long orderId;
    private String remark;
}
