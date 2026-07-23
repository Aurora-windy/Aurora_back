package com.aurora.hr.dept.service;

import com.aurora.hr.dept.model.req.DeptSaveReq;
import com.aurora.hr.dept.model.resp.DeptResp;

import java.util.List;

public interface HrDeptService {

    List<DeptResp> list();

    Long add(DeptSaveReq req);

    void update(Long id, DeptSaveReq req);

    void delete(Long id);
}
