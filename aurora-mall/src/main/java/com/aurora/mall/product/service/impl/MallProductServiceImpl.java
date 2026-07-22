package com.aurora.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.aurora.common.response.PageResult;
import com.aurora.mall.enums.ProductStatusEnum;
import com.aurora.mall.product.entity.MallProductDO;
import com.aurora.mall.product.mapper.MallProductMapper;
import com.aurora.mall.product.model.req.ProductPageReq;
import com.aurora.mall.product.model.req.ProductSaveReq;
import com.aurora.mall.product.model.req.ProductStatusReq;
import com.aurora.mall.product.model.resp.ProductResp;
import com.aurora.mall.product.service.MallProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MallProductServiceImpl implements MallProductService {

    private final MallProductMapper productMapper;

    @Override
    public PageResult<ProductResp> page(ProductPageReq req) {
        return pageBy(req, false);
    }

    @Override
    public PageResult<ProductResp> available(ProductPageReq req) {
        return pageBy(req, true);
    }

    @Override
    public ProductResp detail(Long id) {
        return toResp(requireProduct(id));
    }

    @Override
    public Long add(ProductSaveReq req) {
        MallProductDO product = new MallProductDO();
        fillProduct(product, req);
        product.setStatus(req.getStatus() == null ? ProductStatusEnum.OFF_SHELF.getCode() : req.getStatus());
        productMapper.insert(product);
        return product.getId();
    }

    @Override
    public void update(Long id, ProductSaveReq req) {
        requireProduct(id);
        MallProductDO product = new MallProductDO();
        product.setId(id);
        fillProduct(product, req);
        product.setStatus(req.getStatus());
        productMapper.updateById(product);
    }

    @Override
    public void updateStatus(Long id, ProductStatusReq req) {
        requireProduct(id);
        if (ProductStatusEnum.fromCode(req.getStatus()) == null) {
            throw new BizException(BizCode.PARAM_ERROR);
        }
        MallProductDO product = new MallProductDO();
        product.setId(id);
        product.setStatus(req.getStatus());
        productMapper.updateById(product);
    }

    @Override
    public void delete(Long id) {
        requireProduct(id);
        productMapper.deleteById(id);
    }

    private PageResult<ProductResp> pageBy(ProductPageReq req, boolean availableOnly) {
        LambdaQueryWrapper<MallProductDO> wrapper = Wrappers.<MallProductDO>lambdaQuery()
                .like(StringUtils.hasText(req.getName()), MallProductDO::getName, req.getName())
                .eq(req.getStatus() != null, MallProductDO::getStatus, req.getStatus())
                .eq(availableOnly, MallProductDO::getStatus, ProductStatusEnum.ON_SHELF.getCode())
                .orderByDesc(MallProductDO::getCreateTime);
        Page<MallProductDO> page = productMapper.selectPage(new Page<>(req.normalizedPageNum(), req.normalizedPageSize()), wrapper);
        List<ProductResp> list = page.getRecords().stream().map(this::toResp).toList();
        return new PageResult<>(list, page.getTotal());
    }

    private void fillProduct(MallProductDO product, ProductSaveReq req) {
        if (req.getStock() == null || req.getStock() < 0 || req.getPrice() == null || req.getPrice().signum() <= 0) {
            throw new BizException(BizCode.PARAM_ERROR);
        }
        product.setName(req.getName());
        product.setDescription(req.getDescription());
        product.setPrice(req.getPrice());
        product.setStock(req.getStock());
    }

    private MallProductDO requireProduct(Long id) {
        MallProductDO product = productMapper.selectById(id);
        if (product == null) {
            throw new BizException(BizCode.DATA_NOT_FOUND);
        }
        return product;
    }

    private ProductResp toResp(MallProductDO product) {
        return ProductResp.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .status(product.getStatus())
                .createTime(product.getCreateTime())
                .build();
    }
}
