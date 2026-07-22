package com.aurora.mall.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aurora.common.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_product")
public class MallProductDO extends BaseDO {
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private Integer status;
}
