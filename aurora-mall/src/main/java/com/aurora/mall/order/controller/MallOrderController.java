package com.aurora.mall.order.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.common.constant.PermCodeConst;
import com.aurora.common.response.PageResult;
import com.aurora.common.response.Result;
import com.aurora.mall.order.model.req.OrderCreateReq;
import com.aurora.mall.order.model.req.OrderPageReq;
import com.aurora.mall.order.model.resp.OrderResp;
import com.aurora.mall.order.service.MallOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mall/orders")
public class MallOrderController {

    private final MallOrderService orderService;

    @SaCheckPermission(PermCodeConst.Mall.Order.PLACE)
    @PostMapping
    public Result<Long> create(@RequestBody OrderCreateReq req) {
        return Result.ok(orderService.create(req));
    }

    @SaCheckPermission(PermCodeConst.Mall.Order.VIEW_MY)
    @GetMapping("/my")
    public Result<PageResult<OrderResp>> myOrders(OrderPageReq req) {
        return Result.ok(orderService.myOrders(req));
    }

    @SaCheckPermission(PermCodeConst.Mall.Order.LIST)
    @GetMapping
    public Result<PageResult<OrderResp>> page(OrderPageReq req) {
        return Result.ok(orderService.page(req));
    }
}
