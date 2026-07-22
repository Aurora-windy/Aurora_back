package com.aurora.mall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.aurora.common.response.PageResult;
import com.aurora.common.util.SecurityUtil;
import com.aurora.mall.cart.entity.MallCartDO;
import com.aurora.mall.cart.mapper.MallCartMapper;
import com.aurora.mall.enums.OrderStatusEnum;
import com.aurora.mall.enums.ProductStatusEnum;
import com.aurora.mall.order.entity.MallOrderDO;
import com.aurora.mall.order.entity.MallOrderItemDO;
import com.aurora.mall.order.mapper.MallOrderItemMapper;
import com.aurora.mall.order.mapper.MallOrderMapper;
import com.aurora.mall.order.model.req.OrderCreateReq;
import com.aurora.mall.order.model.req.OrderPageReq;
import com.aurora.mall.order.model.resp.OrderItemResp;
import com.aurora.mall.order.model.resp.OrderResp;
import com.aurora.mall.order.service.MallOrderService;
import com.aurora.mall.product.entity.MallProductDO;
import com.aurora.mall.product.mapper.MallProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MallOrderServiceImpl implements MallOrderService {

    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final MallCartMapper cartMapper;
    private final MallProductMapper productMapper;
    private final MallOrderMapper orderMapper;
    private final MallOrderItemMapper orderItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(OrderCreateReq req) {
        Long userId = SecurityUtil.requireUserId();
        List<MallCartDO> carts = selectOrderCarts(userId, req.getCartIds());
        if (carts.isEmpty()) {
            throw new BizException(BizCode.PARAM_ERROR, "购物车为空");
        }

        MallOrderDO order = new MallOrderDO();
        order.setOrderNo("MO" + LocalDateTime.now().format(ORDER_NO_FORMATTER));
        order.setUserId(userId);
        order.setStatus(OrderStatusEnum.PENDING_PAYMENT.getCode());
        order.setTotalAmount(BigDecimal.ZERO);
        orderMapper.insert(order);

        BigDecimal total = BigDecimal.ZERO;
        for (MallCartDO cart : carts) {
            MallProductDO product = productMapper.selectById(cart.getProductId());
            if (product == null || product.getStatus() == null || product.getStatus() != ProductStatusEnum.ON_SHELF.getCode()) {
                throw new BizException(BizCode.DATA_NOT_FOUND, "商品不存在或已下架");
            }
            if (cart.getQuantity() == null || cart.getQuantity() < 1) {
                throw new BizException(BizCode.PARAM_ERROR);
            }
            if (productMapper.deductStock(product.getId(), cart.getQuantity()) != 1) {
                throw new BizException(BizCode.STOCK_NOT_ENOUGH);
            }
            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity()));
            MallOrderItemDO item = new MallOrderItemDO();
            item.setOrderId(order.getId());
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setPrice(product.getPrice());
            item.setQuantity(cart.getQuantity());
            item.setSubtotalAmount(subtotal);
            orderItemMapper.insert(item);
            total = total.add(subtotal);
            cartMapper.deleteById(cart.getId());
        }

        order.setTotalAmount(total);
        orderMapper.updateById(order);
        return order.getId();
    }

    @Override
    public PageResult<OrderResp> myOrders(OrderPageReq req) {
        req.setUserId(SecurityUtil.requireUserId());
        return page(req);
    }

    @Override
    public PageResult<OrderResp> page(OrderPageReq req) {
        LambdaQueryWrapper<MallOrderDO> wrapper = Wrappers.<MallOrderDO>lambdaQuery()
                .eq(req.getUserId() != null, MallOrderDO::getUserId, req.getUserId())
                .like(StringUtils.hasText(req.getOrderNo()), MallOrderDO::getOrderNo, req.getOrderNo())
                .eq(req.getStatus() != null, MallOrderDO::getStatus, req.getStatus())
                .orderByDesc(MallOrderDO::getCreateTime);
        Page<MallOrderDO> page = orderMapper.selectPage(new Page<>(req.normalizedPageNum(), req.normalizedPageSize()), wrapper);
        return new PageResult<>(page.getRecords().stream().map(this::toResp).toList(), page.getTotal());
    }

    private List<MallCartDO> selectOrderCarts(Long userId, List<Long> cartIds) {
        LambdaQueryWrapper<MallCartDO> wrapper = Wrappers.<MallCartDO>lambdaQuery()
                .eq(MallCartDO::getUserId, userId)
                .in(cartIds != null && !cartIds.isEmpty(), MallCartDO::getId, cartIds)
                .orderByAsc(MallCartDO::getCreateTime);
        return cartMapper.selectList(wrapper);
    }

    private OrderResp toResp(MallOrderDO order) {
        List<OrderItemResp> items = orderItemMapper.selectList(Wrappers.<MallOrderItemDO>lambdaQuery()
                        .eq(MallOrderItemDO::getOrderId, order.getId())
                        .orderByAsc(MallOrderItemDO::getCreateTime))
                .stream().map(item -> OrderItemResp.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .subtotalAmount(item.getSubtotalAmount())
                        .build())
                .toList();
        return OrderResp.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .createTime(order.getCreateTime())
                .items(items)
                .build();
    }
}
