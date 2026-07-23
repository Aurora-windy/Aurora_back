package com.aurora.hr.position.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.common.constant.PermCodeConst;
import com.aurora.common.response.Result;
import com.aurora.hr.position.model.req.PositionSaveReq;
import com.aurora.hr.position.model.resp.PositionResp;
import com.aurora.hr.position.service.HrPositionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/hr/positions")
public class HrPositionController {

    private final HrPositionService positionService;

    @SaCheckPermission(PermCodeConst.Hr.Position.LIST)
    @GetMapping
    public Result<List<PositionResp>> list(@RequestParam(required = false) Long deptId) {
        return Result.ok(positionService.list(deptId));
    }

    @SaCheckPermission(PermCodeConst.Hr.Position.ADD)
    @PostMapping
    public Result<Long> add(@RequestBody @Valid PositionSaveReq req) {
        return Result.ok(positionService.add(req));
    }

    @SaCheckPermission(PermCodeConst.Hr.Position.EDIT)
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody @Valid PositionSaveReq req) {
        positionService.update(id, req);
        return Result.ok(Boolean.TRUE);
    }

    @SaCheckPermission(PermCodeConst.Hr.Position.REMOVE)
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        positionService.delete(id);
        return Result.ok(Boolean.TRUE);
    }
}
