package com.aurora.edu.course.model.req;

import com.aurora.common.base.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CoursePageReq extends PageRequest {
    private String courseCode;
    private String name;
    private Long teacherId;
    private Integer category;
    private Integer status;
}
