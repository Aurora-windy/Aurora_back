package com.aurora.mall.cart.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.common.constant.PermCodeConst;
import com.aurora.common.response.Result;
import com.aurora.mall.cart.model.req.CartAddReq;
import com.aurora.mall.cart.model.req.CartUpdateReq;
import com.aurora.mall.cart.model.resp.CartResp;
import com.aurora.mall.cart.service.MallCartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mall/cart")
public class MallCartController {

    private final MallCartService cartService;

    @SaCheckPermission(PermCodeConst.Mall.Cart.LIST)
    @GetMapping
    public Result<List<CartResp>> list() {
        return Result.ok(cartService.list());
    }

    @SaCheckPermission(PermCodeConst.Mall.Cart.ADD)
    @PostMapping
    public Result<Long> add(@RequestBody @Valid CartAddReq req) {
        return Result.ok(cartService.add(req));
    }

    @SaCheckPermission(PermCodeConst.Mall.Cart.EDIT)
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody @Valid CartUpdateReq req) {
        cartService.update(id, req);
        return Result.ok(Boolean.TRUE);
    }

    @SaCheckPermission(PermCodeConst.Mall.Cart.REMOVE)
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        cartService.delete(id);
        return Result.ok(Boolean.TRUE);
    }
}
