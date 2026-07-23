package com.aurora.hr.employee.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.aurora.common.response.PageResult;
import com.aurora.hr.employee.entity.HrEmployeeDO;
import com.aurora.hr.employee.mapper.HrEmployeeMapper;
import com.aurora.hr.employee.model.req.EmployeePageReq;
import com.aurora.hr.employee.model.req.EmployeeSaveReq;
import com.aurora.hr.employee.model.resp.EmployeeResp;
import com.aurora.hr.employee.service.HrEmployeeService;
import com.aurora.hr.enums.EmpStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HrEmployeeServiceImpl implements HrEmployeeService {

    private final HrEmployeeMapper employeeMapper;

    @Override
    public PageResult<EmployeeResp> page(EmployeePageReq req) {
        LambdaQueryWrapper<HrEmployeeDO> wrapper = Wrappers.<HrEmployeeDO>lambdaQuery()
                .eq(req.getDeptId() != null, HrEmployeeDO::getDeptId, req.getDeptId())
                .eq(req.getStatus() != null, HrEmployeeDO::getStatus, req.getStatus())
                .like(StringUtils.hasText(req.getKeyword()), HrEmployeeDO::getName, req.getKeyword())
                .orderByDesc(HrEmployeeDO::getCreateTime);
        Page<HrEmployeeDO> page = employeeMapper.selectPage(new Page<>(req.normalizedPageNum(), req.normalizedPageSize()), wrapper);
        List<EmployeeResp> list = page.getRecords().stream().map(this::toResp).toList();
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public EmployeeResp detail(Long id) {
        return toResp(requireEmployee(id));
    }

    @Override
    public Long add(EmployeeSaveReq req) {
        checkEmpNoUnique(req.getEmpNo(), null);
        HrEmployeeDO employee = new HrEmployeeDO();
        fillEmployee(employee, req);
        employee.setStatus(req.getStatus() == null ? EmpStatusEnum.ACTIVE.getCode() : req.getStatus());
        employeeMapper.insert(employee);
        return employee.getId();
    }

    @Override
    public void update(Long id, EmployeeSaveReq req) {
        requireEmployee(id);
        checkEmpNoUnique(req.getEmpNo(), id);
        HrEmployeeDO employee = new HrEmployeeDO();
        employee.setId(id);
        fillEmployee(employee, req);
        employee.setStatus(req.getStatus());
        employeeMapper.updateById(employee);
    }

    @Override
    public void delete(Long id) {
        requireEmployee(id);
        employeeMapper.deleteById(id);
    }

    private void checkEmpNoUnique(String empNo, Long excludeId) {
        Long count = employeeMapper.selectCount(Wrappers.<HrEmployeeDO>lambdaQuery()
                .eq(HrEmployeeDO::getEmpNo, empNo)
                .ne(excludeId != null, HrEmployeeDO::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException(BizCode.EMP_NO_DUPLICATED);
        }
    }

    private void fillEmployee(HrEmployeeDO employee, EmployeeSaveReq req) {
        employee.setEmpNo(req.getEmpNo());
        employee.setUserId(req.getUserId());
        employee.setDeptId(req.getDeptId());
        employee.setPositionId(req.getPositionId());
        employee.setName(req.getName());
        employee.setGender(req.getGender());
        employee.setPhone(req.getPhone());
        employee.setEntryDate(req.getEntryDate());
    }

    private HrEmployeeDO requireEmployee(Long id) {
        HrEmployeeDO employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw new BizException(BizCode.DATA_NOT_FOUND);
        }
        return employee;
    }

    private EmployeeResp toResp(HrEmployeeDO employee) {
        return EmployeeResp.builder()
                .id(employee.getId())
                .empNo(employee.getEmpNo())
                .userId(employee.getUserId())
                .deptId(employee.getDeptId())
                .positionId(employee.getPositionId())
                .name(employee.getName())
                .gender(employee.getGender())
                .phone(employee.getPhone())
                .entryDate(employee.getEntryDate())
                .status(employee.getStatus())
                .createTime(employee.getCreateTime())
                .build();
    }
}
