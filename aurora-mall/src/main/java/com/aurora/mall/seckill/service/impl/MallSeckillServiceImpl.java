package com.aurora.mall.seckill.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aurora.common.constant.RedisKeyConst;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.aurora.common.response.PageResult;
import com.aurora.mall.order.entity.MallOrderDO;
import com.aurora.mall.order.entity.MallOrderItemDO;
import com.aurora.mall.order.mapper.MallOrderMapper;
import com.aurora.mall.order.mapper.MallOrderItemMapper;
import com.aurora.mall.product.entity.MallProductDO;
import com.aurora.mall.product.mapper.MallProductMapper;
import com.aurora.mall.seckill.entity.MallSeckillActivityDO;
import com.aurora.mall.seckill.entity.MallStockLogDO;
import com.aurora.mall.seckill.mapper.MallSeckillActivityMapper;
import com.aurora.mall.seckill.mapper.MallStockLogMapper;
import com.aurora.mall.seckill.model.req.SeckillActivitySaveReq;
import com.aurora.mall.seckill.model.req.SeckillJoinReq;
import com.aurora.mall.seckill.model.resp.SeckillActivityResp;
import com.aurora.mall.seckill.model.resp.StockLogResp;
import com.aurora.mall.seckill.service.MallSeckillService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MallSeckillServiceImpl implements MallSeckillService {

    private static final String DEDUCT_STOCK_LUA =
        "local stock = tonumber(redis.call('get', KEYS[1])) " +
        "if stock == nil then return -1 end " +
        "if stock < 1 then return 0 end " +
        "redis.call('decr', KEYS[1]) " +
        "return 1";

    private final MallSeckillActivityMapper activityMapper;
    private final MallStockLogMapper stockLogMapper;
    private final MallProductMapper productMapper;
    private final MallOrderMapper orderMapper;
    private final MallOrderItemMapper orderItemMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createActivity(SeckillActivitySaveReq req) {
        MallProductDO product = productMapper.selectById(req.getProductId());
        if (product == null) throw new BizException(BizCode.DATA_NOT_FOUND);
        MallSeckillActivityDO a = new MallSeckillActivityDO();
        a.setProductId(req.getProductId());
        a.setSeckillPrice(req.getSeckillPrice());
        a.setSeckillStock(req.getSeckillStock());
        a.setAvailableStock(req.getSeckillStock());
        a.setLimitPerUser(req.getLimitPerUser());
        a.setStartTime(req.getStartTime());
        a.setEndTime(req.getEndTime());
        a.setStatus(1);
        activityMapper.insert(a);
        redisTemplate.opsForValue().set(RedisKeyConst.STOCK_PRODUCT + "seckill:" + a.getId(), String.valueOf(req.getSeckillStock()));
        return a.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateActivity(Long id, SeckillActivitySaveReq req) {
        MallSeckillActivityDO a = activityMapper.selectById(id);
        if (a == null) throw new BizException(BizCode.DATA_NOT_FOUND);
        a.setProductId(req.getProductId());
        a.setSeckillPrice(req.getSeckillPrice());
        a.setSeckillStock(req.getSeckillStock());
        a.setAvailableStock(req.getSeckillStock());
        a.setLimitPerUser(req.getLimitPerUser());
        a.setStartTime(req.getStartTime());
        a.setEndTime(req.getEndTime());
        activityMapper.updateById(a);
        redisTemplate.opsForValue().set(RedisKeyConst.STOCK_PRODUCT + "seckill:" + id, String.valueOf(req.getSeckillStock()));
    }

    @Override
    public void toggleStatus(Long id, Integer status) {
        MallSeckillActivityDO a = activityMapper.selectById(id);
        if (a == null) throw new BizException(BizCode.DATA_NOT_FOUND);
        a.setStatus(status);
        activityMapper.updateById(a);
        String key = RedisKeyConst.STOCK_PRODUCT + "seckill:" + id;
        if (status == 0) redisTemplate.delete(key);
        else redisTemplate.opsForValue().set(key, String.valueOf(a.getAvailableStock()));
    }

    @Override
    public PageResult<SeckillActivityResp> listActivities(int pageNum, int pageSize) {
        Page<MallSeckillActivityDO> page = activityMapper.selectPage(new Page<>(pageNum, pageSize),
            Wrappers.<MallSeckillActivityDO>lambdaQuery().orderByDesc(MallSeckillActivityDO::getCreateTime));
        return new PageResult<>(page.getRecords().stream().map(this::toResp).toList(), page.getTotal());
    }

    @Override
    public List<SeckillActivityResp> availableActivities() {
        LocalDateTime now = LocalDateTime.now();
        return activityMapper.selectList(Wrappers.<MallSeckillActivityDO>lambdaQuery()
            .eq(MallSeckillActivityDO::getStatus, 1)
            .le(MallSeckillActivityDO::getStartTime, now)
            .ge(MallSeckillActivityDO::getEndTime, now)
            .gt(MallSeckillActivityDO::getAvailableStock, 0))
            .stream().map(this::toResp).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long joinSeckill(SeckillJoinReq req) {
        Long userId = StpUtil.getLoginIdAsLong();
        MallSeckillActivityDO activity = activityMapper.selectById(req.getActivityId());
        if (activity == null) throw new BizException(BizCode.DATA_NOT_FOUND);
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime())) throw new BizException(BizCode.SECKILL_NOT_STARTED);
        if (now.isAfter(activity.getEndTime())) throw new BizException(BizCode.SECKILL_FINISHED);

        String idemKey = RedisKeyConst.IDEM + req.getIdempotentId();
        Boolean idemSet = redisTemplate.opsForValue().setIfAbsent(idemKey, "1", 5, TimeUnit.MINUTES);
        if (idemSet == null || !idemSet) throw new BizException(BizCode.IDEMPOTENT_REJECT);

        String userKey = RedisKeyConst.SECKILL_USER + activity.getId() + ":" + userId;
        Boolean userSet = redisTemplate.opsForValue().setIfAbsent(userKey, "1", 24, TimeUnit.HOURS);
        if (userSet == null || !userSet) throw new BizException(BizCode.SECKILL_LIMIT_EXCEEDED);

        String stockKey = RedisKeyConst.STOCK_PRODUCT + "seckill:" + activity.getId();
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(DEDUCT_STOCK_LUA, Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(stockKey));
        if (result == null || result <= 0) {
            redisTemplate.delete(userKey);
            throw new BizException(BizCode.STOCK_NOT_ENOUGH);
        }

        try {
            activity.setAvailableStock(activity.getAvailableStock() - 1);
            activityMapper.updateById(activity);
            MallProductDO product = productMapper.selectById(activity.getProductId());
            MallOrderDO order = new MallOrderDO();
            order.setOrderNo("SK" + System.currentTimeMillis() + userId);
            order.setUserId(userId);
            order.setTotalAmount(activity.getSeckillPrice());
            order.setStatus(0);
            order.setIsSeckill(1);
            order.setActivityId(activity.getId());
            orderMapper.insert(order);
            MallOrderItemDO item = new MallOrderItemDO();
            item.setOrderId(order.getId());
            item.setProductId(activity.getProductId());
            item.setProductName(product != null ? product.getName() : "seckill");
            item.setPrice(activity.getSeckillPrice());
            item.setQuantity(1);
            item.setSubtotalAmount(activity.getSeckillPrice());
            orderItemMapper.insert(item);
            MallStockLogDO log = new MallStockLogDO();
            log.setProductId(activity.getProductId());
            log.setBizType(1);
            log.setQuantity(-1);
            log.setOrderId(order.getId());
            log.setRemark("seckill deduct");
            stockLogMapper.insert(log);
            return order.getId();
        } catch (Exception e) {
            redisTemplate.opsForValue().increment(stockKey);
            redisTemplate.delete(userKey);
            throw e;
        }
    }

    @Override
    public PageResult<StockLogResp> stockLogs(Long productId, Integer bizType, int pageNum, int pageSize) {
        Page<MallStockLogDO> page = stockLogMapper.selectPage(new Page<>(pageNum, pageSize),
            Wrappers.<MallStockLogDO>lambdaQuery()
                .eq(productId != null, MallStockLogDO::getProductId, productId)
                .eq(bizType != null, MallStockLogDO::getBizType, bizType)
                .orderByDesc(MallStockLogDO::getCreateTime));
        List<Long> pIds = page.getRecords().stream().map(MallStockLogDO::getProductId).distinct().toList();
        Map<Long, String> nameMap = pIds.isEmpty() ? Map.of() :
            productMapper.selectBatchIds(pIds).stream().collect(Collectors.toMap(MallProductDO::getId, MallProductDO::getName));
        List<StockLogResp> list = page.getRecords().stream().map(l -> StockLogResp.builder()
            .id(l.getId()).productId(l.getProductId()).productName(nameMap.get(l.getProductId()))
            .bizType(l.getBizType()).quantity(l.getQuantity()).orderId(l.getOrderId())
            .remark(l.getRemark()).createTime(l.getCreateTime()).build()).toList();
        return new PageResult<>(list, page.getTotal());
    }

    private SeckillActivityResp toResp(MallSeckillActivityDO a) {
        MallProductDO p = productMapper.selectById(a.getProductId());
        return SeckillActivityResp.builder().id(a.getId()).productId(a.getProductId())
            .productName(p != null ? p.getName() : null).seckillPrice(a.getSeckillPrice())
            .originalPrice(p != null ? p.getPrice() : null).seckillStock(a.getSeckillStock())
            .availableStock(a.getAvailableStock()).limitPerUser(a.getLimitPerUser())
            .startTime(a.getStartTime()).endTime(a.getEndTime())
            .status(a.getStatus()).createTime(a.getCreateTime()).build();
    }
}
