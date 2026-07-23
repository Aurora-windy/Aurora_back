package com.aurora.hr.dept.model.resp;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DeptResp {

    private Long id;
    private Long parentId;
    private String deptName;
    private String deptCode;
    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;
}
