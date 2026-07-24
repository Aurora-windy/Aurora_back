package com.aurora.mall.seckill.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.common.constant.PermCodeConst;
import com.aurora.common.response.PageResult;
import com.aurora.common.response.Result;
import com.aurora.mall.seckill.model.req.SeckillActivitySaveReq;
import com.aurora.mall.seckill.model.req.SeckillJoinReq;
import com.aurora.mall.seckill.model.resp.SeckillActivityResp;
import com.aurora.mall.seckill.model.resp.StockLogResp;
import com.aurora.mall.seckill.service.MallSeckillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mall/seckill")
public class MallSeckillController {

    private final MallSeckillService seckillService;

    @SaCheckPermission(PermCodeConst.Mall.Product.ADD)
    @PostMapping("/activities")
    public Result<Long> create(@RequestBody @Valid SeckillActivitySaveReq req) {
        return Result.ok(seckillService.createActivity(req));
    }

    @SaCheckPermission(PermCodeConst.Mall.Product.EDIT)
    @PutMapping("/activities/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody @Valid SeckillActivitySaveReq req) {
        seckillService.updateActivity(id, req);
        return Result.ok(true);
    }

    @SaCheckPermission(PermCodeConst.Mall.Product.STATUS)
    @PatchMapping("/activities/{id}/status")
    public Result<Boolean> toggle(@PathVariable Long id, @RequestParam Integer status) {
        seckillService.toggleStatus(id, status);
        return Result.ok(true);
    }

    @SaCheckPermission(PermCodeConst.Mall.Product.LIST)
    @GetMapping("/activities")
    public Result<PageResult<SeckillActivityResp>> list(@RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(seckillService.listActivities(pageNum, pageSize));
    }

    @SaCheckPermission(PermCodeConst.Mall.SECKILL_JOIN)
    @GetMapping("/available")
    public Result<List<SeckillActivityResp>> available() {
        return Result.ok(seckillService.availableActivities());
    }

    @SaCheckPermission(PermCodeConst.Mall.SECKILL_JOIN)
    @PostMapping("/join")
    public Result<Long> join(@RequestBody @Valid SeckillJoinReq req) {
        return Result.ok(seckillService.joinSeckill(req));
    }

    @SaCheckPermission(PermCodeConst.Mall.Product.LIST)
    @GetMapping("/stock-logs")
    public Result<PageResult<StockLogResp>> stockLogs(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Integer bizType,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(seckillService.stockLogs(productId, bizType, pageNum, pageSize));
    }
}
