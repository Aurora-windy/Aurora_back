package com.aurora.hr.attendance.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交申诉请求。
 */
@Data
public class AppealSubmitReq {

    @NotNull(message = "考勤记录ID不能为空")
    private Long attendanceId;

    @NotBlank(message = "申诉理由不能为空")
    private String reason;
}
