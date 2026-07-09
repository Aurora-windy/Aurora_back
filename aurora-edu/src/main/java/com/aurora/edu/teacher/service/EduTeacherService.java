package com.aurora.edu.teacher.service;

import com.aurora.common.response.PageResult;
import com.aurora.edu.teacher.model.req.TeacherAddReq;
import com.aurora.edu.teacher.model.req.TeacherPageReq;
import com.aurora.edu.teacher.model.req.TeacherUpdateReq;
import com.aurora.edu.teacher.model.resp.TeacherResp;

public interface EduTeacherService {
    PageResult<TeacherResp> page(TeacherPageReq req);
    TeacherResp detail(Long id);
    Long add(TeacherAddReq req);
    void update(TeacherUpdateReq req);
    void delete(Long id);
}
