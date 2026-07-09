package com.aurora.edu.teacher.model.req;

import com.aurora.common.base.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TeacherPageReq extends PageRequest {
    private String teacherNo;
    private String name;
    private Integer status;
}
