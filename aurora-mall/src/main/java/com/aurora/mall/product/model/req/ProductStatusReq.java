package com.aurora.mall.product.model.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductStatusReq {
    @NotNull(message = "状态不能为空")
    private Integer status;
}
