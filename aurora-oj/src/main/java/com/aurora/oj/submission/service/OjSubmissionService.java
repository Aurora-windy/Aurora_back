package com.aurora.oj.submission.service;

import com.aurora.common.response.PageResult;
import com.aurora.oj.submission.model.req.SubmissionCreateReq;
import com.aurora.oj.submission.model.req.SubmissionPageReq;
import com.aurora.oj.submission.model.resp.SubmissionResp;

public interface OjSubmissionService {

    Long submit(SubmissionCreateReq req);

    PageResult<SubmissionResp> mySubmissions(SubmissionPageReq req);

    PageResult<SubmissionResp> page(SubmissionPageReq req);
}
