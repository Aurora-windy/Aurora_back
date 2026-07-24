package com.aurora.hr.attendance.service;

import com.aurora.common.response.PageResult;
import com.aurora.hr.attendance.model.req.AppealAuditReq;
import com.aurora.hr.attendance.model.req.AppealPageReq;
import com.aurora.hr.attendance.model.req.AppealSubmitReq;
import com.aurora.hr.attendance.model.req.AttendancePageReq;
import com.aurora.hr.attendance.model.resp.AppealResp;
import com.aurora.hr.attendance.model.resp.AttendanceResp;

import java.util.List;

/**
 * 考勤服务接口。
 *
 * <p>spec: docs/specs/2026-07-24-hr-attendance.md</p>
 */
public interface HrAttendanceService {

    /** 当前用户打卡 */
    AttendanceResp clockIn();

    /** 查看我的考勤记录（按月） */
    PageResult<AttendanceResp> myRecords(Integer year, Integer month, int pageNum, int pageSize);

    /** 管理员查看全部考勤记录 */
    PageResult<AttendanceResp> pageRecords(AttendancePageReq req);

    /** 提交申诉 */
    Long submitAppeal(AppealSubmitReq req);

    /** 查看我的申诉 */
    List<AppealResp> myAppeals();

    /** 管理员查看申诉列表 */
    PageResult<AppealResp> pageAppeals(AppealPageReq req);

    /** 审核申诉 */
    void auditAppeal(Long appealId, AppealAuditReq req);
}
