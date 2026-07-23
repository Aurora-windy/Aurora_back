package com.aurora.hr.position.service;

import com.aurora.hr.position.model.req.PositionSaveReq;
import com.aurora.hr.position.model.resp.PositionResp;

import java.util.List;

public interface HrPositionService {

    List<PositionResp> list(Long deptId);

    Long add(PositionSaveReq req);

    void update(Long id, PositionSaveReq req);

    void delete(Long id);
}
