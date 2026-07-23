package com.aurora.hr.position.model.resp;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PositionResp {

    private Long id;
    private Long deptId;
    private String positionName;
    private String positionCode;
    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;
}
