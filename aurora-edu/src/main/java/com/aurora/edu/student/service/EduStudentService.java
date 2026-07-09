package com.aurora.edu.student.service;

import com.aurora.common.response.PageResult;
import com.aurora.edu.student.model.req.StudentAddReq;
import com.aurora.edu.student.model.req.StudentPageReq;
import com.aurora.edu.student.model.req.StudentUpdateReq;
import com.aurora.edu.student.model.resp.StudentResp;

public interface EduStudentService {
    PageResult<StudentResp> page(StudentPageReq req);
    StudentResp detail(Long id);
    Long add(StudentAddReq req);
    void update(StudentUpdateReq req);
    void delete(Long id);
}
