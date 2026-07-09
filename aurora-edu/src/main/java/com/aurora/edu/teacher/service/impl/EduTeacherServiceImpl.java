package com.aurora.edu.teacher.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.aurora.common.response.PageResult;
import com.aurora.edu.teacher.entity.EduTeacherDO;
import com.aurora.edu.teacher.mapper.EduTeacherMapper;
import com.aurora.edu.teacher.model.req.TeacherAddReq;
import com.aurora.edu.teacher.model.req.TeacherPageReq;
import com.aurora.edu.teacher.model.req.TeacherUpdateReq;
import com.aurora.edu.teacher.model.resp.TeacherResp;
import com.aurora.edu.teacher.service.EduTeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EduTeacherServiceImpl implements EduTeacherService {

    private final EduTeacherMapper teacherMapper;

    @Override
    public PageResult<TeacherResp> page(TeacherPageReq req) {
        LambdaQueryWrapper<EduTeacherDO> wrapper = Wrappers.<EduTeacherDO>lambdaQuery()
                .like(StringUtils.hasText(req.getTeacherNo()), EduTeacherDO::getTeacherNo, req.getTeacherNo())
                .like(StringUtils.hasText(req.getName()), EduTeacherDO::getName, req.getName())
                .eq(req.getStatus() != null, EduTeacherDO::getStatus, req.getStatus())
                .orderByDesc(EduTeacherDO::getCreateTime);
        Page<EduTeacherDO> page = teacherMapper.selectPage(new Page<>(req.normalizedPageNum(), req.normalizedPageSize()), wrapper);
        List<TeacherResp> list = page.getRecords().stream().map(this::toResp).toList();
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public TeacherResp detail(Long id) {
        return toResp(requireTeacher(id));
    }

    @Override
    public Long add(TeacherAddReq req) {
        ensureTeacherNoUnique(req.getTeacherNo(), null);
        EduTeacherDO teacher = new EduTeacherDO();
        teacher.setUserId(req.getUserId());
        teacher.setTeacherNo(req.getTeacherNo());
        teacher.setName(req.getName());
        teacher.setTitle(req.getTitle() == null ? 3 : req.getTitle());
        teacher.setPhone(req.getPhone());
        teacher.setCollege(req.getCollege());
        teacher.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        teacherMapper.insert(teacher);
        return teacher.getId();
    }

    @Override
    public void update(TeacherUpdateReq req) {
        requireTeacher(req.getId());
        ensureTeacherNoUnique(req.getTeacherNo(), req.getId());
        EduTeacherDO teacher = new EduTeacherDO();
        teacher.setId(req.getId());
        teacher.setUserId(req.getUserId());
        teacher.setTeacherNo(req.getTeacherNo());
        teacher.setName(req.getName());
        teacher.setTitle(req.getTitle());
        teacher.setPhone(req.getPhone());
        teacher.setCollege(req.getCollege());
        teacher.setStatus(req.getStatus());
        teacherMapper.updateById(teacher);
    }

    @Override
    public void delete(Long id) {
        requireTeacher(id);
        teacherMapper.deleteById(id);
    }

    private EduTeacherDO requireTeacher(Long id) {
        EduTeacherDO teacher = teacherMapper.selectById(id);
        if (teacher == null) {
            throw new BizException(BizCode.DATA_NOT_FOUND.getCode(), "教师不存在");
        }
        return teacher;
    }

    private void ensureTeacherNoUnique(String teacherNo, Long excludeId) {
        LambdaQueryWrapper<EduTeacherDO> wrapper = Wrappers.<EduTeacherDO>lambdaQuery()
                .eq(EduTeacherDO::getTeacherNo, teacherNo)
                .ne(excludeId != null, EduTeacherDO::getId, excludeId);
        if (teacherMapper.selectCount(wrapper) > 0) {
            throw new BizException(BizCode.TEACHER_NO_DUPLICATED);
        }
    }

    private TeacherResp toResp(EduTeacherDO teacher) {
        return TeacherResp.builder()
                .id(teacher.getId())
                .userId(teacher.getUserId())
                .teacherNo(teacher.getTeacherNo())
                .name(teacher.getName())
                .title(teacher.getTitle())
                .phone(teacher.getPhone())
                .college(teacher.getCollege())
                .status(teacher.getStatus())
                .createTime(teacher.getCreateTime())
                .build();
    }
}
