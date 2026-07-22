package com.aurora.mall.product.service;

import com.aurora.common.response.PageResult;
import com.aurora.mall.product.model.req.ProductPageReq;
import com.aurora.mall.product.model.req.ProductSaveReq;
import com.aurora.mall.product.model.req.ProductStatusReq;
import com.aurora.mall.product.model.resp.ProductResp;

public interface MallProductService {
    PageResult<ProductResp> page(ProductPageReq req);
    PageResult<ProductResp> available(ProductPageReq req);
    ProductResp detail(Long id);
    Long add(ProductSaveReq req);
    void update(Long id, ProductSaveReq req);
    void updateStatus(Long id, ProductStatusReq req);
    void delete(Long id);
}
