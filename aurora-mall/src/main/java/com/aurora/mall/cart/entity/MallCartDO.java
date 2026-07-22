package com.aurora.mall.cart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aurora.common.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_cart")
public class MallCartDO extends BaseDO {
    private Long userId;
    private Long productId;
    private Integer quantity;
}
