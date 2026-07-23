package com.aurora.hr.dept.model.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeptSaveReq {

    private Long id;
    private Long parentId;
    @NotBlank(message = "部门名称不能为空")
    private String deptName;
    private String deptCode;
    private Integer sort;
    private Integer status;
}
