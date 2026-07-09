package com.aurora.edu.selection.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.aurora.common.util.SecurityUtil;
import com.aurora.edu.course.entity.EduCourseDO;
import com.aurora.edu.course.mapper.EduCourseMapper;
import com.aurora.edu.selection.entity.EduSelectionDO;
import com.aurora.edu.selection.mapper.EduSelectionMapper;
import com.aurora.edu.selection.model.resp.SelectionResp;
import com.aurora.edu.selection.service.EduSelectionService;
import com.aurora.edu.student.entity.EduStudentDO;
import com.aurora.edu.student.mapper.EduStudentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EduSelectionServiceImpl implements EduSelectionService {
    private static final int SELECTED = 1;
    private static final int DROPPED = 2;

    private final EduSelectionMapper selectionMapper;
    private final EduCourseMapper courseMapper;
    private final EduStudentMapper studentMapper;

    @Override
    public List<SelectionResp> mySelections() {
        EduStudentDO student = requireCurrentStudent();
        List<EduSelectionDO> selections = selectionMapper.selectList(Wrappers.<EduSelectionDO>lambdaQuery()
                .eq(EduSelectionDO::getStudentId, student.getId())
                .orderByDesc(EduSelectionDO::getSelectedTime));
        return selections.stream().map(this::toResp).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long selectCourse(Long courseId) {
        EduStudentDO student = requireCurrentStudent();
        EduCourseDO course = requireSelectableCourse(courseId);
        EduSelectionDO existing = selectByStudentAndCourse(student.getId(), course.getId());
        if (existing != null && SELECTED == existing.getStatus()) {
            throw new BizException(BizCode.DUPLICATE_SELECTION);
        }
        int capacity = course.getCapacity() == null ? 0 : course.getCapacity();
        int updated = courseMapper.update(null, Wrappers.<EduCourseDO>lambdaUpdate()
                .setIncrBy(EduCourseDO::getSelectedCount, 1)
                .eq(EduCourseDO::getId, course.getId())
                .eq(EduCourseDO::getStatus, 1)
                .lt(EduCourseDO::getSelectedCount, capacity));
        if (updated != 1) {
            throw new BizException(BizCode.COURSE_FULL);
        }
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            int reselected = selectionMapper.update(null, Wrappers.<EduSelectionDO>lambdaUpdate()
                    .set(EduSelectionDO::getStatus, SELECTED)
                    .set(EduSelectionDO::getSelectedTime, now)
                    .set(EduSelectionDO::getDroppedTime, null)
                    .eq(EduSelectionDO::getId, existing.getId())
                    .eq(EduSelectionDO::getStatus, DROPPED));
            if (reselected != 1) {
                throw new BizException(BizCode.DUPLICATE_SELECTION);
            }
            return existing.getId();
        }
        EduSelectionDO selection = new EduSelectionDO();
        selection.setStudentId(student.getId());
        selection.setCourseId(course.getId());
        selection.setStatus(SELECTED);
        selection.setSelectedTime(now);
        selectionMapper.insert(selection);
        return selection.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dropCourse(Long courseId) {
        EduStudentDO student = requireCurrentStudent();
        int dropped = selectionMapper.update(null, Wrappers.<EduSelectionDO>lambdaUpdate()
                .set(EduSelectionDO::getStatus, DROPPED)
                .set(EduSelectionDO::getDroppedTime, LocalDateTime.now())
                .eq(EduSelectionDO::getStudentId, student.getId())
                .eq(EduSelectionDO::getCourseId, courseId)
                .eq(EduSelectionDO::getStatus, SELECTED));
        if (dropped != 1) {
            throw new BizException(BizCode.DATA_NOT_FOUND);
        }
        courseMapper.update(null, Wrappers.<EduCourseDO>lambdaUpdate()
                .setDecrBy(EduCourseDO::getSelectedCount, 1)
                .eq(EduCourseDO::getId, courseId)
                .gt(EduCourseDO::getSelectedCount, 0));
    }

    private EduStudentDO requireCurrentStudent() {
        Long userId = SecurityUtil.requireUserId();
        EduStudentDO student = studentMapper.selectList(Wrappers.<EduStudentDO>lambdaQuery()
                .eq(EduStudentDO::getUserId, userId)).stream().findFirst().orElse(null);
        if (student == null) {
            throw new BizException(BizCode.DATA_NOT_FOUND);
        }
        return student;
    }

    private EduCourseDO requireSelectableCourse(Long courseId) {
        EduCourseDO course = courseMapper.selectById(courseId);
        if (course == null || course.getStatus() == null || course.getStatus() != 1) {
            throw new BizException(BizCode.DATA_NOT_FOUND);
        }
        LocalDateTime now = LocalDateTime.now();
        if ((course.getSelectionStartTime() != null && now.isBefore(course.getSelectionStartTime()))
                || (course.getSelectionEndTime() != null && now.isAfter(course.getSelectionEndTime()))) {
            throw new BizException(BizCode.NOT_IN_SELECTION_PERIOD);
        }
        return course;
    }

    private EduSelectionDO selectByStudentAndCourse(Long studentId, Long courseId) {
        return selectionMapper.selectList(Wrappers.<EduSelectionDO>lambdaQuery()
                .eq(EduSelectionDO::getStudentId, studentId)
                .eq(EduSelectionDO::getCourseId, courseId)).stream().findFirst().orElse(null);
    }

    private SelectionResp toResp(EduSelectionDO selection) {
        EduCourseDO course = courseMapper.selectById(selection.getCourseId());
        return SelectionResp.builder()
                .id(selection.getId())
                .studentId(selection.getStudentId())
                .courseId(selection.getCourseId())
                .courseName(course == null ? null : course.getName())
                .status(selection.getStatus())
                .selectedTime(selection.getSelectedTime())
                .droppedTime(selection.getDroppedTime())
                .build();
    }
}
