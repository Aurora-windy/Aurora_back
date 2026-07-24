package com.aurora.hr.attendance.model.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审核申诉请求。
 */
@Data
public class AppealAuditReq {

    /** 审核结果：1=通过, 2=驳回 */
    @NotNull(message = "审核结果不能为空")
    private Integer status;

    private String auditRemark;
}
