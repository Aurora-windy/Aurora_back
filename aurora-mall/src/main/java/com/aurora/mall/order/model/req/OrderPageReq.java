package com.aurora.mall.order.model.req;

import com.aurora.common.base.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderPageReq extends PageRequest {
    private String orderNo;
    private Long userId;
    private Integer status;
}
