package com.aurora.hr.attendance.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.aurora.hr.attendance.entity.HrAttendanceDO;
import com.aurora.hr.attendance.mapper.HrAttendanceMapper;
import com.aurora.hr.employee.entity.HrEmployeeDO;
import com.aurora.hr.employee.mapper.HrEmployeeMapper;
import com.aurora.hr.enums.AttendanceStatusEnum;
import com.aurora.hr.enums.EmpStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 考勤缺卡定时任务。
 *
 * <p>每日 23:50 扫描当日无记录或只有单卡的在职员工，自动标记缺卡。</p>
 * <p>通过 attendance.task.enabled=true 启用，开发环境默认关闭。</p>
 *
 * <p>spec: docs/specs/2026-07-24-hr-attendance.md</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "attendance.task.enabled", havingValue = "true", matchIfMissing = false)
public class AttendanceScheduledTask {

    private final HrAttendanceMapper attendanceMapper;
    private final HrEmployeeMapper employeeMapper;

    @Scheduled(cron = "0 50 23 * * ?")
    public void markAbsent() {
        log.info("考勤缺卡定时任务开始执行");
        LocalDate today = LocalDate.now();

        // 查询所有在职员工（在职 + 试用）
        List<HrEmployeeDO> activeEmployees = employeeMapper.selectList(
                Wrappers.<HrEmployeeDO>lambdaQuery()
                        .in(HrEmployeeDO::getStatus,
                                EmpStatusEnum.ACTIVE.getCode(),
                                EmpStatusEnum.PROBATION.getCode()));

        // 查询今日已有考勤记录
        List<HrAttendanceDO> existingRecords = attendanceMapper.selectList(
                Wrappers.<HrAttendanceDO>lambdaQuery()
                        .eq(HrAttendanceDO::getAttendanceDate, today));

        Set<Long> recordedEmpIds = existingRecords.stream()
                .map(HrAttendanceDO::getEmpId)
                .collect(Collectors.toSet());

        int created = 0;
        int updated = 0;

        for (HrEmployeeDO emp : activeEmployees) {
            if (!recordedEmpIds.contains(emp.getId())) {
                // 无记录 → 创建缺卡记录
                HrAttendanceDO record = new HrAttendanceDO();
                record.setEmpId(emp.getId());
                record.setAttendanceDate(today);
                record.setStatus(AttendanceStatusEnum.MISSING.getCode());
                record.setRemark("定时任务自动标记缺卡");
                attendanceMapper.insert(record);
                created++;
            }
        }

        // 检查只有单卡的记录
        for (HrAttendanceDO record : existingRecords) {
            if (record.getClockInTime() == null && record.getClockOutTime() != null) {
                // 只有下班卡无上班卡 → 缺卡
                record.setStatus(AttendanceStatusEnum.MISSING.getCode());
                record.setRemark("缺少上班打卡记录");
                attendanceMapper.updateById(record);
                updated++;
            }
        }

        log.info("考勤缺卡定时任务完成: 新建缺卡{}条, 更新缺卡{}条", created, updated);
    }
}
