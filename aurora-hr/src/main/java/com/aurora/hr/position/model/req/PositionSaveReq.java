package com.aurora.hr.position.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PositionSaveReq {

    private Long id;
    @NotNull(message = "所属部门不能为空")
    private Long deptId;
    @NotBlank(message = "岗位名称不能为空")
    private String positionName;
    private String positionCode;
    private Integer sort;
    private Integer status;
}
