package com.aurora.hr.attendance.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aurora.common.constant.RedisKeyConst;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.aurora.common.response.PageResult;
import com.aurora.hr.attendance.entity.HrAttendanceAppealDO;
import com.aurora.hr.attendance.entity.HrAttendanceDO;
import com.aurora.hr.attendance.mapper.HrAttendanceAppealMapper;
import com.aurora.hr.attendance.mapper.HrAttendanceMapper;
import com.aurora.hr.attendance.model.req.AppealAuditReq;
import com.aurora.hr.attendance.model.req.AppealPageReq;
import com.aurora.hr.attendance.model.req.AppealSubmitReq;
import com.aurora.hr.attendance.model.req.AttendancePageReq;
import com.aurora.hr.attendance.model.resp.AppealResp;
import com.aurora.hr.attendance.model.resp.AttendanceResp;
import com.aurora.hr.attendance.service.HrAttendanceService;
import com.aurora.hr.employee.entity.HrEmployeeDO;
import com.aurora.hr.employee.mapper.HrEmployeeMapper;
import com.aurora.hr.enums.AppealStatusEnum;
import com.aurora.hr.enums.AttendanceStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 考勤服务实现。
 *
 * <p>spec: docs/specs/2026-07-24-hr-attendance.md</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrAttendanceServiceImpl implements HrAttendanceService {

    private static final LocalTime CLOCK_IN_START = LocalTime.of(6, 0);
    private static final LocalTime CLOCK_IN_END = LocalTime.of(12, 0);
    private static final LocalTime CLOCK_OUT_START = LocalTime.of(12, 0);
    private static final LocalTime CLOCK_OUT_END = LocalTime.of(23, 59);
    private static final LocalTime LATE_THRESHOLD = LocalTime.of(9, 0);
    private static final LocalTime EARLY_LEAVE_THRESHOLD = LocalTime.of(18, 0);

    private final HrAttendanceMapper attendanceMapper;
    private final HrAttendanceAppealMapper appealMapper;
    private final HrEmployeeMapper employeeMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttendanceResp clockIn() {
        Long userId = StpUtil.getLoginIdAsLong();
        HrEmployeeDO employee = findEmployeeByUserId(userId);
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();

        // 判断是上班卡还是下班卡
        boolean isClockIn = currentTime.isBefore(CLOCK_IN_END);
        if (currentTime.isBefore(CLOCK_IN_START) || currentTime.isAfter(CLOCK_OUT_END)) {
            throw new BizException(BizCode.NOT_CLOCK_TIME);
        }

        // Redis 锁防并发
        String lockKey = RedisKeyConst.LOCK_ATTENDANCE + employee.getId() + ":" + today;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 60, TimeUnit.SECONDS);
        if (locked == null || !locked) {
            // 降级：依赖 DB 唯一约束
            log.warn("考勤锁获取失败，降级为DB约束: empId={}, date={}", employee.getId(), today);
        }

        try {
            // 查询今日记录
            HrAttendanceDO record = attendanceMapper.selectOne(
                    Wrappers.<HrAttendanceDO>lambdaQuery()
                            .eq(HrAttendanceDO::getEmpId, employee.getId())
                            .eq(HrAttendanceDO::getAttendanceDate, today));

            if (record == null) {
                // 首次打卡，创建记录
                record = new HrAttendanceDO();
                record.setEmpId(employee.getId());
                record.setAttendanceDate(today);
                if (isClockIn) {
                    record.setClockInTime(now);
                    record.setStatus(determineStatus(now, null));
                } else {
                    record.setClockOutTime(now);
                    record.setStatus(determineStatus(null, now));
                }
                attendanceMapper.insert(record);
            } else {
                // 已有记录，补打另一卡
                if (isClockIn) {
                    if (record.getClockInTime() != null) {
                        throw new BizException(BizCode.ALREADY_CLOCKED);
                    }
                    record.setClockInTime(now);
                } else {
                    if (record.getClockOutTime() != null) {
                        throw new BizException(BizCode.ALREADY_CLOCKED);
                    }
                    record.setClockOutTime(now);
                }
                record.setStatus(determineStatus(record.getClockInTime(), record.getClockOutTime()));
                attendanceMapper.updateById(record);
            }

            return toResp(record, employee);
        } finally {
            // 释放锁
            redisTemplate.delete(lockKey);
        }
    }

    @Override
    public PageResult<AttendanceResp> myRecords(Integer year, Integer month, int pageNum, int pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();
        HrEmployeeDO employee = findEmployeeByUserId(userId);

        LambdaQueryWrapper<HrAttendanceDO> wrapper = Wrappers.<HrAttendanceDO>lambdaQuery()
                .eq(HrAttendanceDO::getEmpId, employee.getId());

        if (year != null && month != null) {
            YearMonth ym = YearMonth.of(year, month);
            wrapper.ge(HrAttendanceDO::getAttendanceDate, ym.atDay(1))
                    .le(HrAttendanceDO::getAttendanceDate, ym.atEndOfMonth());
        }
        wrapper.orderByDesc(HrAttendanceDO::getAttendanceDate);

        Page<HrAttendanceDO> page = attendanceMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<AttendanceResp> list = page.getRecords().stream()
                .map(r -> toResp(r, employee))
                .toList();
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public PageResult<AttendanceResp> pageRecords(AttendancePageReq req) {
        LambdaQueryWrapper<HrAttendanceDO> wrapper = Wrappers.<HrAttendanceDO>lambdaQuery()
                .eq(req.getEmpId() != null, HrAttendanceDO::getEmpId, req.getEmpId())
                .eq(req.getStatus() != null, HrAttendanceDO::getStatus, req.getStatus())
                .ge(req.getDateFrom() != null, HrAttendanceDO::getAttendanceDate, req.getDateFrom())
                .le(req.getDateTo() != null, HrAttendanceDO::getAttendanceDate, req.getDateTo())
                .orderByDesc(HrAttendanceDO::getAttendanceDate);

        // 按部门筛选：先查该部门下的员工ID列表
        if (req.getDeptId() != null) {
            List<Long> empIds = employeeMapper.selectList(
                    Wrappers.<HrEmployeeDO>lambdaQuery().eq(HrEmployeeDO::getDeptId, req.getDeptId()))
                    .stream().map(HrEmployeeDO::getId).toList();
            if (empIds.isEmpty()) {
                return new PageResult<>(List.of(), 0L);
            }
            wrapper.in(HrAttendanceDO::getEmpId, empIds);
        }

        Page<HrAttendanceDO> page = attendanceMapper.selectPage(
                new Page<>(req.normalizedPageNum(), req.normalizedPageSize()), wrapper);

        // 批量查员工信息
        List<Long> empIds = page.getRecords().stream().map(HrAttendanceDO::getEmpId).distinct().toList();
        Map<Long, HrEmployeeDO> empMap = empIds.isEmpty() ? Map.of() :
                employeeMapper.selectBatchIds(empIds).stream()
                        .collect(Collectors.toMap(HrEmployeeDO::getId, e -> e));

        List<AttendanceResp> list = page.getRecords().stream()
                .map(r -> toResp(r, empMap.get(r.getEmpId())))
                .toList();
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitAppeal(AppealSubmitReq req) {
        Long userId = StpUtil.getLoginIdAsLong();
        HrEmployeeDO employee = findEmployeeByUserId(userId);

        // 校验考勤记录存在且属于当前员工
        HrAttendanceDO attendance = attendanceMapper.selectById(req.getAttendanceId());
        if (attendance == null || !attendance.getEmpId().equals(employee.getId())) {
            throw new BizException(BizCode.DATA_NOT_FOUND);
        }

        HrAttendanceAppealDO appeal = new HrAttendanceAppealDO();
        appeal.setAttendanceId(req.getAttendanceId());
        appeal.setEmpId(employee.getId());
        appeal.setReason(req.getReason());
        appeal.setStatus(AppealStatusEnum.PENDING.getCode());
        appealMapper.insert(appeal);
        return appeal.getId();
    }

    @Override
    public List<AppealResp> myAppeals() {
        Long userId = StpUtil.getLoginIdAsLong();
        HrEmployeeDO employee = findEmployeeByUserId(userId);

        List<HrAttendanceAppealDO> appeals = appealMapper.selectList(
                Wrappers.<HrAttendanceAppealDO>lambdaQuery()
                        .eq(HrAttendanceAppealDO::getEmpId, employee.getId())
                        .orderByDesc(HrAttendanceAppealDO::getCreateTime));

        return appeals.stream().map(a -> toAppealResp(a, employee, null)).toList();
    }

    @Override
    public PageResult<AppealResp> pageAppeals(AppealPageReq req) {
        LambdaQueryWrapper<HrAttendanceAppealDO> wrapper = Wrappers.<HrAttendanceAppealDO>lambdaQuery()
                .eq(req.getStatus() != null, HrAttendanceAppealDO::getStatus, req.getStatus())
                .orderByDesc(HrAttendanceAppealDO::getCreateTime);

        Page<HrAttendanceAppealDO> page = appealMapper.selectPage(
                new Page<>(req.normalizedPageNum(), req.normalizedPageSize()), wrapper);

        List<Long> empIds = page.getRecords().stream().map(HrAttendanceAppealDO::getEmpId).distinct().toList();
        Map<Long, HrEmployeeDO> empMap = empIds.isEmpty() ? Map.of() :
                employeeMapper.selectBatchIds(empIds).stream()
                        .collect(Collectors.toMap(HrEmployeeDO::getId, e -> e));

        // 批量查考勤记录获取日期
        List<Long> attIds = page.getRecords().stream().map(HrAttendanceAppealDO::getAttendanceId).distinct().toList();
        Map<Long, HrAttendanceDO> attMap = attIds.isEmpty() ? Map.of() :
                attendanceMapper.selectBatchIds(attIds).stream()
                        .collect(Collectors.toMap(HrAttendanceDO::getId, a -> a));

        List<AppealResp> list = page.getRecords().stream()
                .map(a -> toAppealResp(a, empMap.get(a.getEmpId()), attMap.get(a.getAttendanceId())))
                .toList();
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditAppeal(Long appealId, AppealAuditReq req) {
        HrAttendanceAppealDO appeal = appealMapper.selectById(appealId);
        if (appeal == null) {
            throw new BizException(BizCode.APPEAL_NOT_FOUND);
        }
        if (!appeal.getStatus().equals(AppealStatusEnum.PENDING.getCode())) {
            throw new BizException(BizCode.APPEAL_NOT_FOUND);
        }

        Long auditorId = StpUtil.getLoginIdAsLong();
        appeal.setStatus(req.getStatus());
        appeal.setAuditUserId(auditorId);
        appeal.setAuditRemark(req.getAuditRemark());
        appeal.setAuditTime(LocalDateTime.now());
        appealMapper.updateById(appeal);

        // 通过 → 更新考勤状态为"正常(已审批)"
        if (AppealStatusEnum.APPROVED.getCode() == req.getStatus()) {
            HrAttendanceDO attendance = attendanceMapper.selectById(appeal.getAttendanceId());
            if (attendance != null) {
                attendance.setStatus(AttendanceStatusEnum.APPROVED.getCode());
                attendanceMapper.updateById(attendance);
            }
        }
    }

    // ===== private helpers =====

    private HrEmployeeDO findEmployeeByUserId(Long userId) {
        HrEmployeeDO employee = employeeMapper.selectOne(
                Wrappers.<HrEmployeeDO>lambdaQuery().eq(HrEmployeeDO::getUserId, userId));
        if (employee == null) {
            throw new BizException(BizCode.DATA_NOT_FOUND);
        }
        return employee;
    }

    private int determineStatus(LocalDateTime clockIn, LocalDateTime clockOut) {
        boolean late = clockIn != null && clockIn.toLocalTime().isAfter(LATE_THRESHOLD);
        boolean earlyLeave = clockOut != null && clockOut.toLocalTime().isBefore(EARLY_LEAVE_THRESHOLD);

        if (late) {
            return AttendanceStatusEnum.LATE.getCode();
        }
        if (earlyLeave) {
            return AttendanceStatusEnum.EARLY_LEAVE.getCode();
        }
        // 只有单卡时暂判正常，等定时任务补判
        return AttendanceStatusEnum.NORMAL.getCode();
    }

    private AttendanceResp toResp(HrAttendanceDO record, HrEmployeeDO employee) {
        return AttendanceResp.builder()
                .id(record.getId())
                .empId(record.getEmpId())
                .empName(employee != null ? employee.getName() : null)
                .empNo(employee != null ? employee.getEmpNo() : null)
                .attendanceDate(record.getAttendanceDate())
                .clockInTime(record.getClockInTime())
                .clockOutTime(record.getClockOutTime())
                .status(record.getStatus())
                .remark(record.getRemark())
                .createTime(record.getCreateTime())
                .build();
    }

    private AppealResp toAppealResp(HrAttendanceAppealDO appeal, HrEmployeeDO employee, HrAttendanceDO attendance) {
        return AppealResp.builder()
                .id(appeal.getId())
                .attendanceId(appeal.getAttendanceId())
                .empId(appeal.getEmpId())
                .empName(employee != null ? employee.getName() : null)
                .empNo(employee != null ? employee.getEmpNo() : null)
                .attendanceDate(attendance != null ? attendance.getAttendanceDate() : null)
                .attendanceStatus(attendance != null ? attendance.getStatus() : null)
                .reason(appeal.getReason())
                .status(appeal.getStatus())
                .auditUserId(appeal.getAuditUserId())
                .auditRemark(appeal.getAuditRemark())
                .auditTime(appeal.getAuditTime())
                .createTime(appeal.getCreateTime())
                .build();
    }
}
