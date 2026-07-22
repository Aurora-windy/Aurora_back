package com.aurora.oj.problem.service;

import com.aurora.common.response.PageResult;
import com.aurora.oj.problem.model.req.ProblemPageReq;
import com.aurora.oj.problem.model.req.ProblemSaveReq;
import com.aurora.oj.problem.model.resp.ProblemResp;

public interface OjProblemService {

    PageResult<ProblemResp> page(ProblemPageReq req);

    PageResult<ProblemResp> available(ProblemPageReq req);

    ProblemResp detail(Long id);

    ProblemResp availableDetail(Long id);

    Long add(ProblemSaveReq req);

    void update(Long id, ProblemSaveReq req);

    void delete(Long id);
}
