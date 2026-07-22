package com.aurora.mall.product.model.req;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSaveReq {
    private Long id;
    @NotBlank(message = "商品名称不能为空")
    private String name;
    private String description;
    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "商品价格必须大于 0")
    private BigDecimal price;
    @NotNull(message = "商品库存不能为空")
    @Min(value = 0, message = "商品库存不能小于 0")
    private Integer stock;
    private Integer status;
}
