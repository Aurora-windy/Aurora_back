package com.aurora.mall.product.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.common.constant.PermCodeConst;
import com.aurora.common.response.PageResult;
import com.aurora.common.response.Result;
import com.aurora.mall.product.model.req.ProductPageReq;
import com.aurora.mall.product.model.req.ProductSaveReq;
import com.aurora.mall.product.model.req.ProductStatusReq;
import com.aurora.mall.product.model.resp.ProductResp;
import com.aurora.mall.product.service.MallProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mall/products")
public class MallProductController {

    private final MallProductService productService;

    @SaCheckPermission(PermCodeConst.Mall.Product.LIST)
    @GetMapping
    public Result<PageResult<ProductResp>> page(ProductPageReq req) {
        return Result.ok(productService.page(req));
    }

    @SaCheckPermission(PermCodeConst.Mall.Product.LIST)
    @GetMapping("/available")
    public Result<PageResult<ProductResp>> available(ProductPageReq req) {
        return Result.ok(productService.available(req));
    }

    @SaCheckPermission(PermCodeConst.Mall.Product.LIST)
    @GetMapping("/{id}")
    public Result<ProductResp> detail(@PathVariable Long id) {
        return Result.ok(productService.detail(id));
    }

    @SaCheckPermission(PermCodeConst.Mall.Product.ADD)
    @PostMapping
    public Result<Long> add(@RequestBody @Valid ProductSaveReq req) {
        return Result.ok(productService.add(req));
    }

    @SaCheckPermission(PermCodeConst.Mall.Product.EDIT)
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody @Valid ProductSaveReq req) {
        productService.update(id, req);
        return Result.ok(Boolean.TRUE);
    }

    @SaCheckPermission(PermCodeConst.Mall.Product.STATUS)
    @PatchMapping("/{id}/status")
    public Result<Boolean> updateStatus(@PathVariable Long id, @RequestBody @Valid ProductStatusReq req) {
        productService.updateStatus(id, req);
        return Result.ok(Boolean.TRUE);
    }

    @SaCheckPermission(PermCodeConst.Mall.Product.REMOVE)
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.ok(Boolean.TRUE);
    }
}
