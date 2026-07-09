package com.aurora.edu.student.model.req;

import com.aurora.common.base.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StudentPageReq extends PageRequest {
    private String studentNo;
    private String name;
    private Integer status;
}
