package com.aurora.hr.attendance.model.req;

import com.aurora.common.base.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 申诉记录分页查询请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AppealPageReq extends PageRequest {

    private Integer status;
}
