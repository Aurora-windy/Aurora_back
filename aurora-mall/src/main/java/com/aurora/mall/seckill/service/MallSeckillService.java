package com.aurora.mall.seckill.service;

import com.aurora.common.response.PageResult;
import com.aurora.mall.seckill.model.req.SeckillActivitySaveReq;
import com.aurora.mall.seckill.model.req.SeckillJoinReq;
import com.aurora.mall.seckill.model.resp.SeckillActivityResp;
import com.aurora.mall.seckill.model.resp.StockLogResp;
import java.util.List;

public interface MallSeckillService {
    Long createActivity(SeckillActivitySaveReq req);
    void updateActivity(Long id, SeckillActivitySaveReq req);
    void toggleStatus(Long id, Integer status);
    PageResult<SeckillActivityResp> listActivities(int pageNum, int pageSize);
    List<SeckillActivityResp> availableActivities();
    Long joinSeckill(SeckillJoinReq req);
    PageResult<StockLogResp> stockLogs(Long productId, Integer bizType, int pageNum, int pageSize);
}
