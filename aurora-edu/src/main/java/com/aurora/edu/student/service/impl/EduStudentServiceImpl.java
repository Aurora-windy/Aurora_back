package com.aurora.edu.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aurora.common.constant.RoleCodeConst;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.aurora.common.response.PageResult;
import com.aurora.common.util.SecurityUtil;
import com.aurora.edu.student.entity.EduStudentDO;
import com.aurora.edu.student.mapper.EduStudentMapper;
import com.aurora.edu.student.model.req.StudentAccountOptionReq;
import com.aurora.edu.student.model.req.StudentAddReq;
import com.aurora.edu.student.model.req.StudentPageReq;
import com.aurora.edu.student.model.req.StudentUpdateReq;
import com.aurora.edu.student.model.resp.StudentAccountOptionResp;
import com.aurora.edu.student.model.resp.StudentProfileStatusResp;
import com.aurora.edu.student.model.resp.StudentResp;
import com.aurora.edu.student.service.EduStudentService;
import com.aurora.system.user.entity.SysUserDO;
import com.aurora.system.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EduStudentServiceImpl implements EduStudentService {

    private final EduStudentMapper studentMapper;
    private final SysUserMapper userMapper;

    @Override
    public PageResult<StudentResp> page(StudentPageReq req) {
        LambdaQueryWrapper<EduStudentDO> wrapper = Wrappers.<EduStudentDO>lambdaQuery()
                .like(StringUtils.hasText(req.getStudentNo()), EduStudentDO::getStudentNo, req.getStudentNo())
                .like(StringUtils.hasText(req.getName()), EduStudentDO::getName, req.getName())
                .eq(req.getStatus() != null, EduStudentDO::getStatus, req.getStatus())
                .orderByDesc(EduStudentDO::getCreateTime);
        Page<EduStudentDO> page = studentMapper.selectPage(new Page<>(req.normalizedPageNum(), req.normalizedPageSize()), wrapper);
        List<StudentResp> list = page.getRecords().stream().map(this::toResp).toList();
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public StudentResp detail(Long id) {
        return toResp(requireStudent(id));
    }

    @Override
    public Long add(StudentAddReq req) {
        ensureStudentNoUnique(req.getStudentNo(), null);
        validateUserBinding(req.getUserId(), null);
        EduStudentDO student = new EduStudentDO();
        student.setUserId(req.getUserId());
        student.setStudentNo(req.getStudentNo());
        student.setName(req.getName());
        student.setGender(req.getGender());
        student.setPhone(req.getPhone());
        student.setMajor(req.getMajor());
        student.setClassName(req.getClassName());
        student.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        studentMapper.insert(student);
        return student.getId();
    }

    @Override
    public void update(StudentUpdateReq req) {
        requireStudent(req.getId());
        ensureStudentNoUnique(req.getStudentNo(), req.getId());
        validateUserBinding(req.getUserId(), req.getId());
        EduStudentDO student = new EduStudentDO();
        student.setId(req.getId());
        student.setUserId(req.getUserId());
        student.setStudentNo(req.getStudentNo());
        student.setName(req.getName());
        student.setGender(req.getGender());
        student.setPhone(req.getPhone());
        student.setMajor(req.getMajor());
        student.setClassName(req.getClassName());
        student.setStatus(req.getStatus());
        studentMapper.updateById(student);
    }

    @Override
    public void delete(Long id) {
        requireStudent(id);
        studentMapper.deleteById(id);
    }

    @Override
    public List<StudentAccountOptionResp> listBindableUsers(StudentAccountOptionReq req) {
        return studentMapper.selectBindableUsers(
                req.normalizedKeyword(),
                req.getCurrentStudentId(),
                req.normalizedLimit());
    }

    @Override
    public StudentProfileStatusResp currentProfileStatus() {
        Long userId = SecurityUtil.requireUserId();
        EduStudentDO student = studentMapper.selectByUserId(userId);
        if (student == null) {
            return StudentProfileStatusResp.builder()
                    .bound(false)
                    .enabled(false)
                    .message(BizCode.STUDENT_PROFILE_NOT_BOUND.getMsg())
                    .build();
        }
        if (student.getStatus() == null || student.getStatus() != 1) {
            return StudentProfileStatusResp.builder()
                    .bound(true)
                    .enabled(false)
                    .studentId(student.getId())
                    .message(BizCode.STUDENT_PROFILE_DISABLED.getMsg())
                    .build();
        }
        return StudentProfileStatusResp.builder()
                .bound(true)
                .enabled(true)
                .studentId(student.getId())
                .build();
    }

    private EduStudentDO requireStudent(Long id) {
        EduStudentDO student = studentMapper.selectById(id);
        if (student == null) {
            throw new BizException(BizCode.DATA_NOT_FOUND.getCode(), "学生不存在");
        }
        return student;
    }

    private void ensureStudentNoUnique(String studentNo, Long excludeId) {
        LambdaQueryWrapper<EduStudentDO> wrapper = Wrappers.<EduStudentDO>lambdaQuery()
                .eq(EduStudentDO::getStudentNo, studentNo)
                .ne(excludeId != null, EduStudentDO::getId, excludeId);
        if (studentMapper.selectCount(wrapper) > 0) {
            throw new BizException(BizCode.STUDENT_NO_DUPLICATED);
        }
    }

    private void validateUserBinding(Long userId, Long excludeStudentId) {
        if (userId == null) {
            return;
        }
        SysUserDO user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(BizCode.STUDENT_USER_NOT_FOUND);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(BizCode.STUDENT_USER_DISABLED);
        }
        if (!userMapper.selectRoleCodesByUserId(userId).contains(RoleCodeConst.STUDENT)) {
            throw new BizException(BizCode.STUDENT_USER_ROLE_INVALID);
        }
        LambdaQueryWrapper<EduStudentDO> wrapper = Wrappers.<EduStudentDO>lambdaQuery()
                .eq(EduStudentDO::getUserId, userId)
                .ne(excludeStudentId != null, EduStudentDO::getId, excludeStudentId);
        if (studentMapper.selectCount(wrapper) > 0) {
            throw new BizException(BizCode.STUDENT_USER_ALREADY_BOUND);
        }
    }

    private StudentResp toResp(EduStudentDO student) {
        return StudentResp.builder()
                .id(student.getId())
                .userId(student.getUserId())
                .studentNo(student.getStudentNo())
                .name(student.getName())
                .gender(student.getGender())
                .phone(student.getPhone())
                .major(student.getMajor())
                .className(student.getClassName())
                .status(student.getStatus())
                .createTime(student.getCreateTime())
                .build();
    }
}