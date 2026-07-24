package com.aurora.mall.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.common.constant.PermCodeConst;
import com.aurora.common.response.Result;
import com.aurora.mall.payment.service.MallPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mall/orders")
public class MallPaymentController {

    private final MallPaymentService paymentService;

    @SaCheckPermission(PermCodeConst.Mall.Order.VIEW_MY)
    @PostMapping("/{id}/pay")
    public Result<Boolean> pay(@PathVariable Long id) {
        paymentService.pay(id);
        return Result.ok(true);
    }

    @SaCheckPermission(PermCodeConst.Mall.Order.VIEW_MY)
    @PostMapping("/{id}/cancel")
    public Result<Boolean> cancel(@PathVariable Long id) {
        paymentService.cancel(id);
        return Result.ok(true);
    }

    @SaCheckPermission(PermCodeConst.Mall.Order.SHIP)
    @PostMapping("/{id}/ship")
    public Result<Boolean> ship(@PathVariable Long id) {
        paymentService.ship(id);
        return Result.ok(true);
    }

    @SaCheckPermission(PermCodeConst.Mall.Order.VIEW_MY)
    @PostMapping("/{id}/complete")
    public Result<Boolean> complete(@PathVariable Long id) {
        paymentService.complete(id);
        return Result.ok(true);
    }

    @SaCheckPermission(PermCodeConst.Mall.Order.REFUND)
    @PostMapping("/{id}/refund")
    public Result<Boolean> refund(@PathVariable Long id) {
        paymentService.refund(id);
        return Result.ok(true);
    }
}
