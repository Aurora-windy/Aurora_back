package com.aurora.mall.cart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.aurora.common.util.SecurityUtil;
import com.aurora.mall.cart.entity.MallCartDO;
import com.aurora.mall.cart.mapper.MallCartMapper;
import com.aurora.mall.cart.model.req.CartAddReq;
import com.aurora.mall.cart.model.req.CartUpdateReq;
import com.aurora.mall.cart.model.resp.CartResp;
import com.aurora.mall.cart.service.MallCartService;
import com.aurora.mall.enums.ProductStatusEnum;
import com.aurora.mall.product.entity.MallProductDO;
import com.aurora.mall.product.mapper.MallProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MallCartServiceImpl implements MallCartService {

    private final MallCartMapper cartMapper;
    private final MallProductMapper productMapper;

    @Override
    public List<CartResp> list() {
        Long userId = SecurityUtil.requireUserId();
        return cartMapper.selectList(Wrappers.<MallCartDO>lambdaQuery()
                        .eq(MallCartDO::getUserId, userId)
                        .orderByDesc(MallCartDO::getCreateTime))
                .stream().map(this::toResp).toList();
    }

    @Override
    public Long add(CartAddReq req) {
        Long userId = SecurityUtil.requireUserId();
        MallProductDO product = requireAvailableProduct(req.getProductId());
        if (req.getQuantity() > product.getStock()) {
            throw new BizException(BizCode.STOCK_NOT_ENOUGH);
        }
        LambdaQueryWrapper<MallCartDO> wrapper = Wrappers.<MallCartDO>lambdaQuery()
                .eq(MallCartDO::getUserId, userId)
                .eq(MallCartDO::getProductId, req.getProductId());
        MallCartDO existing = cartMapper.selectOne(wrapper);
        if (existing != null) {
            int quantity = existing.getQuantity() + req.getQuantity();
            if (quantity > product.getStock()) {
                throw new BizException(BizCode.STOCK_NOT_ENOUGH);
            }
            existing.setQuantity(quantity);
            cartMapper.updateById(existing);
            return existing.getId();
        }
        MallCartDO cart = new MallCartDO();
        cart.setUserId(userId);
        cart.setProductId(req.getProductId());
        cart.setQuantity(req.getQuantity());
        cartMapper.insert(cart);
        return cart.getId();
    }

    @Override
    public void update(Long id, CartUpdateReq req) {
        MallCartDO cart = requireOwnCart(id);
        MallProductDO product = requireAvailableProduct(cart.getProductId());
        if (req.getQuantity() > product.getStock()) {
            throw new BizException(BizCode.STOCK_NOT_ENOUGH);
        }
        cart.setQuantity(req.getQuantity());
        cartMapper.updateById(cart);
    }

    @Override
    public void delete(Long id) {
        requireOwnCart(id);
        cartMapper.deleteById(id);
    }

    private MallCartDO requireOwnCart(Long id) {
        Long userId = SecurityUtil.requireUserId();
        MallCartDO cart = cartMapper.selectById(id);
        if (cart == null || !userId.equals(cart.getUserId())) {
            throw new BizException(BizCode.DATA_NOT_FOUND);
        }
        return cart;
    }

    private MallProductDO requireAvailableProduct(Long productId) {
        MallProductDO product = productMapper.selectById(productId);
        if (product == null || product.getStatus() == null || product.getStatus() != ProductStatusEnum.ON_SHELF.getCode()) {
            throw new BizException(BizCode.DATA_NOT_FOUND);
        }
        return product;
    }

    private CartResp toResp(MallCartDO cart) {
        MallProductDO product = productMapper.selectById(cart.getProductId());
        BigDecimal price = product == null ? BigDecimal.ZERO : product.getPrice();
        return CartResp.builder()
                .id(cart.getId())
                .productId(cart.getProductId())
                .productName(product == null ? "-" : product.getName())
                .price(price)
                .stock(product == null ? 0 : product.getStock())
                .productStatus(product == null ? null : product.getStatus())
                .quantity(cart.getQuantity())
                .subtotalAmount(price.multiply(BigDecimal.valueOf(cart.getQuantity())))
                .build();
    }
}
