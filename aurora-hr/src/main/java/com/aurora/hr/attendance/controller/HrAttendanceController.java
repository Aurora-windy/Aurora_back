package com.aurora.hr.attendance.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.common.constant.PermCodeConst;
import com.aurora.common.response.PageResult;
import com.aurora.common.response.Result;
import com.aurora.hr.attendance.model.req.AppealAuditReq;
import com.aurora.hr.attendance.model.req.AppealPageReq;
import com.aurora.hr.attendance.model.req.AppealSubmitReq;
import com.aurora.hr.attendance.model.req.AttendancePageReq;
import com.aurora.hr.attendance.model.resp.AppealResp;
import com.aurora.hr.attendance.model.resp.AttendanceResp;
import com.aurora.hr.attendance.service.HrAttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 考勤控制器。
 *
 * <p>spec: docs/specs/2026-07-24-hr-attendance.md</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/hr/attendance")
public class HrAttendanceController {

    private final HrAttendanceService attendanceService;

    /** 打卡（员工自助） */
    @SaCheckPermission(PermCodeConst.Hr.Attendance.CLOCK_IN)
    @PostMapping("/clock-in")
    public Result<AttendanceResp> clockIn() {
        return Result.ok(attendanceService.clockIn());
    }

    /** 查看我的考勤（员工自助） */
    @SaCheckPermission(PermCodeConst.Hr.Attendance.CLOCK_IN)
    @GetMapping("/my")
    public Result<PageResult<AttendanceResp>> myRecords(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "31") int pageSize) {
        return Result.ok(attendanceService.myRecords(year, month, pageNum, pageSize));
    }

    /** 查看我的申诉（员工自助） */
    @SaCheckPermission(PermCodeConst.Hr.Attendance.CLOCK_IN)
    @GetMapping("/appeals/my")
    public Result<List<AppealResp>> myAppeals() {
        return Result.ok(attendanceService.myAppeals());
    }

    /** 提交申诉（员工自助） */
    @SaCheckPermission(PermCodeConst.Hr.Attendance.CLOCK_IN)
    @PostMapping("/appeals")
    public Result<Long> submitAppeal(@RequestBody @Valid AppealSubmitReq req) {
        return Result.ok(attendanceService.submitAppeal(req));
    }

    /** 查看全部考勤记录（管理员） */
    @SaCheckPermission(PermCodeConst.Hr.Attendance.AUDIT)
    @GetMapping("/records")
    public Result<PageResult<AttendanceResp>> pageRecords(AttendancePageReq req) {
        return Result.ok(attendanceService.pageRecords(req));
    }

    /** 查看申诉列表（管理员） */
    @SaCheckPermission(PermCodeConst.Hr.Attendance.AUDIT)
    @GetMapping("/appeals")
    public Result<PageResult<AppealResp>> pageAppeals(AppealPageReq req) {
        return Result.ok(attendanceService.pageAppeals(req));
    }

    /** 审核申诉（管理员） */
    @SaCheckPermission(PermCodeConst.Hr.Attendance.AUDIT)
    @PutMapping("/appeals/{id}/audit")
    public Result<Boolean> auditAppeal(@PathVariable Long id, @RequestBody @Valid AppealAuditReq req) {
        attendanceService.auditAppeal(id, req);
        return Result.ok(Boolean.TRUE);
    }
}
