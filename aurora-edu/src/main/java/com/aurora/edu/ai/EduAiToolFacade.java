package com.aurora.edu.ai;

import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.aurora.common.response.PageResult;
import com.aurora.edu.course.entity.EduCourseDO;
import com.aurora.edu.course.mapper.EduCourseMapper;
import com.aurora.edu.course.model.req.CoursePageReq;
import com.aurora.edu.course.model.resp.CourseResp;
import com.aurora.edu.course.service.EduCourseService;
import com.aurora.edu.selection.entity.EduSelectionDO;
import com.aurora.edu.selection.mapper.EduSelectionMapper;
import com.aurora.edu.student.entity.EduStudentDO;
import com.aurora.edu.student.mapper.EduStudentMapper;
import com.aurora.edu.student.model.req.StudentAccountOptionReq;
import com.aurora.edu.student.model.req.StudentPageReq;
import com.aurora.edu.student.model.req.StudentUpdateReq;
import com.aurora.edu.student.model.resp.StudentAccountOptionResp;
import com.aurora.edu.student.model.resp.StudentResp;
import com.aurora.edu.student.service.EduStudentService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EduAiToolFacade {

    private final EduStudentService studentService;
    private final EduCourseService courseService;
    private final EduStudentMapper studentMapper;
    private final EduCourseMapper courseMapper;
    private final EduSelectionMapper selectionMapper;

    public PageResult<StudentResp> searchStudents(StudentPageReq req) {
        return studentService.page(req);
    }

    public List<StudentAccountOptionResp> listBindableUsers(StudentAccountOptionReq req) {
        return studentService.listBindableUsers(req);
    }

    public PageResult<CourseResp> searchCourses(CoursePageReq req) {
        return courseService.page(req);
    }

    public List<EduSelectionDO> listSelectionsByStudent(Long studentId) {
        return selectionMapper.selectList(Wrappers.<EduSelectionDO>lambdaQuery()
                .eq(EduSelectionDO::getStudentId, studentId)
                .orderByDesc(EduSelectionDO::getSelectedTime)
                .orderByDesc(EduSelectionDO::getCreateTime));
    }

    @Transactional(rollbackFor = Exception.class)
    public void bindStudentUser(Long studentId, Long userId) {
        EduStudentDO student = requireStudent(studentId);
        StudentUpdateReq req = toUpdateReq(student);
        req.setUserId(userId);
        studentService.update(req);
    }

    @Transactional(rollbackFor = Exception.class)
    public void unbindStudentUser(Long studentId) {
        EduStudentDO student = requireStudent(studentId);
        StudentUpdateReq req = toUpdateReq(student);
        req.setUserId(null);
        studentService.update(req);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateCapacity(Long courseId, Integer capacity) {
        EduCourseDO course = requireCourse(courseId);
        if (capacity == null || capacity < 0 || capacity < safeInt(course.getSelectedCount())) {
            throw new BizException(BizCode.OPERATION_FAIL, "Course capacity cannot be lower than selected count");
        }
        EduCourseDO update = new EduCourseDO();
        update.setId(courseId);
        update.setCapacity(capacity);
        courseMapper.updateById(update);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateSelectionTime(Long courseId, LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime != null && endTime != null && !startTime.isBefore(endTime)) {
            throw new BizException(BizCode.PARAM_ERROR, "Selection start time must be before end time");
        }
        requireCourse(courseId);
        EduCourseDO update = new EduCourseDO();
        update.setId(courseId);
        update.setSelectionStartTime(startTime);
        update.setSelectionEndTime(endTime);
        courseMapper.updateById(update);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long courseId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(BizCode.PARAM_ERROR, "Course status must be 0 or 1");
        }
        requireCourse(courseId);
        EduCourseDO update = new EduCourseDO();
        update.setId(courseId);
        update.setStatus(status);
        courseMapper.updateById(update);
    }

    @Transactional(rollbackFor = Exception.class)
    public void dropCourseForStudent(Long studentId, Long courseId) {
        EduSelectionDO selection = selectionMapper.selectList(Wrappers.<EduSelectionDO>lambdaQuery()
                .eq(EduSelectionDO::getStudentId, studentId)
                .eq(EduSelectionDO::getCourseId, courseId)
                .eq(EduSelectionDO::getStatus, 1)
                .last("LIMIT 1")).stream().findFirst().orElse(null);
        if (selection == null) {
            throw new BizException(BizCode.DATA_NOT_FOUND, "Active selection does not exist");
        }
        selection.setStatus(2);
        selection.setDroppedTime(LocalDateTime.now());
        selectionMapper.updateById(selection);
        EduCourseDO course = requireCourse(courseId);
        EduCourseDO update = new EduCourseDO();
        update.setId(courseId);
        update.setSelectedCount(Math.max(0, safeInt(course.getSelectedCount()) - 1));
        courseMapper.updateById(update);
    }

    private EduStudentDO requireStudent(Long studentId) {
        EduStudentDO student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BizException(BizCode.DATA_NOT_FOUND, "Student does not exist");
        }
        return student;
    }

    private EduCourseDO requireCourse(Long courseId) {
        EduCourseDO course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BizException(BizCode.DATA_NOT_FOUND, "Course does not exist");
        }
        return course;
    }

    private StudentUpdateReq toUpdateReq(EduStudentDO student) {
        StudentUpdateReq req = new StudentUpdateReq();
        req.setId(student.getId());
        req.setUserId(student.getUserId());
        req.setStudentNo(student.getStudentNo());
        req.setName(student.getName());
        req.setGender(student.getGender());
        req.setPhone(student.getPhone());
        req.setMajor(student.getMajor());
        req.setClassName(student.getClassName());
        req.setStatus(student.getStatus());
        return req;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}