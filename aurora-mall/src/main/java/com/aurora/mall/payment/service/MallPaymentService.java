package com.aurora.mall.payment.service;

public interface MallPaymentService {
    void pay(Long orderId);
    void cancel(Long orderId);
    void ship(Long orderId);
    void complete(Long orderId);
    void refund(Long orderId);
}
