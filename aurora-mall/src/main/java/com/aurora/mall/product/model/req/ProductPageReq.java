package com.aurora.mall.product.model.req;

import com.aurora.common.base.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProductPageReq extends PageRequest {
    private String name;
    private Integer status;
}
