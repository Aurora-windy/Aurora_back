package com.aurora.edu.course.service;

import com.aurora.common.response.PageResult;
import com.aurora.edu.course.model.req.CourseAddReq;
import com.aurora.edu.course.model.req.CoursePageReq;
import com.aurora.edu.course.model.req.CourseUpdateReq;
import com.aurora.edu.course.model.resp.CourseResp;

public interface EduCourseService {
    PageResult<CourseResp> page(CoursePageReq req);
    PageResult<CourseResp> available(CoursePageReq req);
    CourseResp detail(Long id);
    Long add(CourseAddReq req);
    void update(CourseUpdateReq req);
    void delete(Long id);
}
