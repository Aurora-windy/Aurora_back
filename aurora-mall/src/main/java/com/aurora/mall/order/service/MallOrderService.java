package com.aurora.mall.order.service;

import com.aurora.common.response.PageResult;
import com.aurora.mall.order.model.req.OrderCreateReq;
import com.aurora.mall.order.model.req.OrderPageReq;
import com.aurora.mall.order.model.resp.OrderResp;

public interface MallOrderService {
    Long create(OrderCreateReq req);
    PageResult<OrderResp> myOrders(OrderPageReq req);
    PageResult<OrderResp> page(OrderPageReq req);
}
