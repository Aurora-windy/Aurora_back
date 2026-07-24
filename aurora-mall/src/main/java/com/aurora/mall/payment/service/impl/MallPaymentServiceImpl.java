package com.aurora.mall.payment.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.aurora.common.constant.RedisKeyConst;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.aurora.mall.order.entity.MallOrderDO;
import com.aurora.mall.order.entity.MallOrderItemDO;
import com.aurora.mall.order.mapper.MallOrderMapper;
import com.aurora.mall.order.mapper.MallOrderItemMapper;
import com.aurora.mall.product.entity.MallProductDO;
import com.aurora.mall.product.mapper.MallProductMapper;
import com.aurora.mall.payment.service.MallPaymentService;
import com.aurora.mall.seckill.entity.MallStockLogDO;
import com.aurora.mall.seckill.mapper.MallStockLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MallPaymentServiceImpl implements MallPaymentService {

    private static final int PAY_TIMEOUT_MINUTES = 30;

    private final MallOrderMapper orderMapper;
    private final MallOrderItemMapper orderItemMapper;
    private final MallProductMapper productMapper;
    private final MallStockLogMapper stockLogMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long orderId) {
        MallOrderDO order = requireOrder(orderId);
        requireStatus(order, 0);
        if (order.getCreateTime().plusMinutes(PAY_TIMEOUT_MINUTES).isBefore(LocalDateTime.now())) {
            doCancel(order, "支付超时自动取消");
            throw new BizException(BizCode.ORDER_PAID_TIMEOUT);
        }
        String idemKey = RedisKeyConst.IDEM + "order:" + orderId;
        Boolean set = redisTemplate.opsForValue().setIfAbsent(idemKey, "1", 5, TimeUnit.MINUTES);
        if (set == null || !set) throw new BizException(BizCode.IDEMPOTENT_REJECT);
        order.setStatus(1);
        order.setPayType(3);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId) {
        MallOrderDO order = requireOrder(orderId);
        requireStatus(order, 0);
        doCancel(order, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ship(Long orderId) {
        MallOrderDO order = requireOrder(orderId);
        requireStatus(order, 1);
        order.setStatus(2);
        order.setShipTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void complete(Long orderId) {
        MallOrderDO order = requireOrder(orderId);
        requireStatus(order, 2);
        order.setStatus(3);
        order.setCompleteTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(Long orderId) {
        MallOrderDO order = requireOrder(orderId);
        requireStatus(order, 1);
        order.setStatus(5);
        orderMapper.updateById(order);
        restoreStock(order, 3);
    }

    private void doCancel(MallOrderDO order, String reason) {
        order.setStatus(4);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(reason);
        orderMapper.updateById(order);
        restoreStock(order, 2);
    }

    private void restoreStock(MallOrderDO order, int bizType) {
        List<MallOrderItemDO> items = orderItemMapper.selectList(
            Wrappers.<MallOrderItemDO>lambdaQuery().eq(MallOrderItemDO::getOrderId, order.getId()));
        for (MallOrderItemDO item : items) {
            MallProductDO product = productMapper.selectById(item.getProductId());
            if (product != null) {
                product.setStock(product.getStock() + item.getQuantity());
                productMapper.updateById(product);
            }
            MallStockLogDO log = new MallStockLogDO();
            log.setProductId(item.getProductId());
            log.setBizType(bizType);
            log.setQuantity(item.getQuantity());
            log.setOrderId(order.getId());
            log.setRemark(bizType == 2 ? "取消回补" : "退款回补");
            stockLogMapper.insert(log);
        }
    }

    private MallOrderDO requireOrder(Long orderId) {
        MallOrderDO order = orderMapper.selectById(orderId);
        if (order == null) throw new BizException(BizCode.DATA_NOT_FOUND);
        return order;
    }

    private void requireStatus(MallOrderDO order, int expected) {
        if (order.getStatus() != expected) throw new BizException(BizCode.ORDER_STATUS_ILLEGAL);
    }
}
