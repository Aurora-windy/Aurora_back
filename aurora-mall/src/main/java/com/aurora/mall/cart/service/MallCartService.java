package com.aurora.mall.cart.service;

import com.aurora.mall.cart.model.req.CartAddReq;
import com.aurora.mall.cart.model.req.CartUpdateReq;
import com.aurora.mall.cart.model.resp.CartResp;

import java.util.List;

public interface MallCartService {
    List<CartResp> list();
    Long add(CartAddReq req);
    void update(Long id, CartUpdateReq req);
    void delete(Long id);
}
