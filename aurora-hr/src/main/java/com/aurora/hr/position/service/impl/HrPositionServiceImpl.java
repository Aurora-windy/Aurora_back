package com.aurora.hr.position.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.aurora.hr.position.entity.HrPositionDO;
import com.aurora.hr.position.mapper.HrPositionMapper;
import com.aurora.hr.position.model.req.PositionSaveReq;
import com.aurora.hr.position.model.resp.PositionResp;
import com.aurora.hr.position.service.HrPositionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HrPositionServiceImpl implements HrPositionService {

    private final HrPositionMapper positionMapper;

    @Override
    public List<PositionResp> list(Long deptId) {
        LambdaQueryWrapper<HrPositionDO> wrapper = Wrappers.<HrPositionDO>lambdaQuery()
                .eq(deptId != null, HrPositionDO::getDeptId, deptId)
                .orderByAsc(HrPositionDO::getSort)
                .orderByDesc(HrPositionDO::getCreateTime);
        return positionMapper.selectList(wrapper).stream().map(this::toResp).toList();
    }

    @Override
    public Long add(PositionSaveReq req) {
        HrPositionDO position = new HrPositionDO();
        fillPosition(position, req);
        position.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        positionMapper.insert(position);
        return position.getId();
    }

    @Override
    public void update(Long id, PositionSaveReq req) {
        requirePosition(id);
        HrPositionDO position = new HrPositionDO();
        position.setId(id);
        fillPosition(position, req);
        position.setStatus(req.getStatus());
        positionMapper.updateById(position);
    }

    @Override
    public void delete(Long id) {
        // 一期按方案 C：岗位删除不校验员工占用，仅逻辑删除。
        requirePosition(id);
        positionMapper.deleteById(id);
    }

    private void fillPosition(HrPositionDO position, PositionSaveReq req) {
        position.setDeptId(req.getDeptId());
        position.setPositionName(req.getPositionName());
        position.setPositionCode(req.getPositionCode());
        position.setSort(req.getSort() == null ? 0 : req.getSort());
    }

    private HrPositionDO requirePosition(Long id) {
        HrPositionDO position = positionMapper.selectById(id);
        if (position == null) {
            throw new BizException(BizCode.DATA_NOT_FOUND);
        }
        return position;
    }

    private PositionResp toResp(HrPositionDO position) {
        return PositionResp.builder()
                .id(position.getId())
                .deptId(position.getDeptId())
                .positionName(position.getPositionName())
                .positionCode(position.getPositionCode())
                .sort(position.getSort())
                .status(position.getStatus())
                .createTime(position.getCreateTime())
                .build();
    }
}
