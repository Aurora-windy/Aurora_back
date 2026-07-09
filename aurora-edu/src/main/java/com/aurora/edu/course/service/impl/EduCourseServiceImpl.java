package com.aurora.edu.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.aurora.common.response.PageResult;
import com.aurora.edu.course.entity.EduCourseDO;
import com.aurora.edu.course.mapper.EduCourseMapper;
import com.aurora.edu.course.model.req.CourseAddReq;
import com.aurora.edu.course.model.req.CoursePageReq;
import com.aurora.edu.course.model.req.CourseUpdateReq;
import com.aurora.edu.course.model.resp.CourseResp;
import com.aurora.edu.course.service.EduCourseService;
import com.aurora.edu.teacher.mapper.EduTeacherMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EduCourseServiceImpl implements EduCourseService {

    private final EduCourseMapper courseMapper;
    private final EduTeacherMapper teacherMapper;

    @Override
    public PageResult<CourseResp> page(CoursePageReq req) { return pageBy(req, false); }

    @Override
    public PageResult<CourseResp> available(CoursePageReq req) { return pageBy(req, true); }

    @Override
    public CourseResp detail(Long id) { return toResp(requireCourse(id)); }

    @Override
    public Long add(CourseAddReq req) {
        ensureCourseCodeUnique(req.getCourseCode(), null);
        ensureTeacherExists(req.getTeacherId());
        validateCapacity(req.getCapacity(), 0);
        EduCourseDO course = new EduCourseDO();
        course.setCourseCode(req.getCourseCode());
        course.setName(req.getName());
        course.setTeacherId(req.getTeacherId());
        course.setCategory(req.getCategory() == null ? 2 : req.getCategory());
        course.setCredit(req.getCredit() == null ? BigDecimal.ONE : req.getCredit());
        course.setCapacity(req.getCapacity());
        course.setSelectedCount(0);
        course.setSelectionStartTime(req.getSelectionStartTime());
        course.setSelectionEndTime(req.getSelectionEndTime());
        course.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        courseMapper.insert(course);
        return course.getId();
    }

    @Override
    public void update(CourseUpdateReq req) {
        EduCourseDO existing = requireCourse(req.getId());
        ensureCourseCodeUnique(req.getCourseCode(), req.getId());
        ensureTeacherExists(req.getTeacherId());
        validateCapacity(req.getCapacity(), existing.getSelectedCount());
        EduCourseDO course = new EduCourseDO();
        course.setId(req.getId());
        course.setCourseCode(req.getCourseCode());
        course.setName(req.getName());
        course.setTeacherId(req.getTeacherId());
        course.setCategory(req.getCategory());
        course.setCredit(req.getCredit());
        course.setCapacity(req.getCapacity());
        course.setSelectionStartTime(req.getSelectionStartTime());
        course.setSelectionEndTime(req.getSelectionEndTime());
        course.setStatus(req.getStatus());
        courseMapper.updateById(course);
    }

    @Override
    public void delete(Long id) {
        EduCourseDO course = requireCourse(id);
        if (course.getSelectedCount() != null && course.getSelectedCount() > 0) {
            throw new BizException(BizCode.OPERATION_FAIL);
        }
        courseMapper.deleteById(id);
    }

    private PageResult<CourseResp> pageBy(CoursePageReq req, boolean availableOnly) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<EduCourseDO> wrapper = Wrappers.<EduCourseDO>lambdaQuery()
                .like(StringUtils.hasText(req.getCourseCode()), EduCourseDO::getCourseCode, req.getCourseCode())
                .like(StringUtils.hasText(req.getName()), EduCourseDO::getName, req.getName())
                .eq(req.getTeacherId() != null, EduCourseDO::getTeacherId, req.getTeacherId())
                .eq(req.getCategory() != null, EduCourseDO::getCategory, req.getCategory())
                .eq(req.getStatus() != null, EduCourseDO::getStatus, req.getStatus())
                .eq(availableOnly, EduCourseDO::getStatus, 1)
                .and(availableOnly, w -> w.isNull(EduCourseDO::getSelectionStartTime).or().le(EduCourseDO::getSelectionStartTime, now))
                .and(availableOnly, w -> w.isNull(EduCourseDO::getSelectionEndTime).or().ge(EduCourseDO::getSelectionEndTime, now))
                .orderByDesc(EduCourseDO::getCreateTime);
        Page<EduCourseDO> page = courseMapper.selectPage(new Page<>(req.normalizedPageNum(), req.normalizedPageSize()), wrapper);
        List<CourseResp> list = page.getRecords().stream().map(this::toResp).toList();
        return new PageResult<>(list, page.getTotal());
    }

    private EduCourseDO requireCourse(Long id) {
        EduCourseDO course = courseMapper.selectById(id);
        if (course == null) {
            throw new BizException(BizCode.DATA_NOT_FOUND);
        }
        return course;
    }

    private void ensureTeacherExists(Long teacherId) {
        if (teacherMapper.selectById(teacherId) == null) {
            throw new BizException(BizCode.DATA_NOT_FOUND);
        }
    }

    private void ensureCourseCodeUnique(String courseCode, Long excludeId) {
        LambdaQueryWrapper<EduCourseDO> wrapper = Wrappers.<EduCourseDO>lambdaQuery()
                .eq(EduCourseDO::getCourseCode, courseCode)
                .ne(excludeId != null, EduCourseDO::getId, excludeId);
        if (courseMapper.selectCount(wrapper) > 0) {
            throw new BizException(BizCode.DATA_EXISTS);
        }
    }

    private void validateCapacity(Integer capacity, Integer selectedCount) {
        if (capacity == null || capacity < 1) {
            throw new BizException(BizCode.PARAM_ERROR);
        }
        if (selectedCount != null && capacity < selectedCount) {
            throw new BizException(BizCode.PARAM_ERROR);
        }
    }

    private CourseResp toResp(EduCourseDO course) {
        int selected = course.getSelectedCount() == null ? 0 : course.getSelectedCount();
        int capacity = course.getCapacity() == null ? 0 : course.getCapacity();
        return CourseResp.builder()
                .id(course.getId())
                .courseCode(course.getCourseCode())
                .name(course.getName())
                .teacherId(course.getTeacherId())
                .category(course.getCategory())
                .credit(course.getCredit())
                .capacity(course.getCapacity())
                .selectedCount(course.getSelectedCount())
                .remainingCount(Math.max(capacity - selected, 0))
                .selectionStartTime(course.getSelectionStartTime())
                .selectionEndTime(course.getSelectionEndTime())
                .status(course.getStatus())
                .createTime(course.getCreateTime())
                .build();
    }
}
