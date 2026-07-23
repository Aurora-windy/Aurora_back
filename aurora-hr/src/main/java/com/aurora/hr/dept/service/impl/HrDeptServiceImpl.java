package com.aurora.hr.dept.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.aurora.hr.dept.entity.HrDepartmentDO;
import com.aurora.hr.dept.mapper.HrDepartmentMapper;
import com.aurora.hr.dept.model.req.DeptSaveReq;
import com.aurora.hr.dept.model.resp.DeptResp;
import com.aurora.hr.dept.service.HrDeptService;
import com.aurora.hr.employee.entity.HrEmployeeDO;
import com.aurora.hr.employee.mapper.HrEmployeeMapper;
import com.aurora.hr.position.entity.HrPositionDO;
import com.aurora.hr.position.mapper.HrPositionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HrDeptServiceImpl implements HrDeptService {

    private final HrDepartmentMapper deptMapper;
    private final HrPositionMapper positionMapper;
    private final HrEmployeeMapper employeeMapper;

    @Override
    public List<DeptResp> list() {
        LambdaQueryWrapper<HrDepartmentDO> wrapper = Wrappers.<HrDepartmentDO>lambdaQuery()
                .orderByAsc(HrDepartmentDO::getSort)
                .orderByDesc(HrDepartmentDO::getCreateTime);
        return deptMapper.selectList(wrapper).stream().map(this::toResp).toList();
    }

    @Override
    public Long add(DeptSaveReq req) {
        HrDepartmentDO dept = new HrDepartmentDO();
        fillDept(dept, req);
        dept.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        deptMapper.insert(dept);
        return dept.getId();
    }

    @Override
    public void update(Long id, DeptSaveReq req) {
        requireDept(id);
        HrDepartmentDO dept = new HrDepartmentDO();
        dept.setId(id);
        fillDept(dept, req);
        dept.setStatus(req.getStatus());
        deptMapper.updateById(dept);
    }

    @Override
    public void delete(Long id) {
        requireDept(id);
        Long childCount = deptMapper.selectCount(Wrappers.<HrDepartmentDO>lambdaQuery()
                .eq(HrDepartmentDO::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BizException(BizCode.DEPT_HAS_CHILDREN);
        }
        Long posCount = positionMapper.selectCount(Wrappers.<HrPositionDO>lambdaQuery()
                .eq(HrPositionDO::getDeptId, id));
        if (posCount != null && posCount > 0) {
            throw new BizException(BizCode.DEPT_HAS_POSITIONS);
        }
        Long empCount = employeeMapper.selectCount(Wrappers.<HrEmployeeDO>lambdaQuery()
                .eq(HrEmployeeDO::getDeptId, id));
        if (empCount != null && empCount > 0) {
            throw new BizException(BizCode.DEPT_HAS_EMPLOYEES);
        }
        deptMapper.deleteById(id);
    }

    private void fillDept(HrDepartmentDO dept, DeptSaveReq req) {
        dept.setParentId(req.getParentId());
        dept.setDeptName(req.getDeptName());
        dept.setDeptCode(req.getDeptCode());
        dept.setSort(req.getSort() == null ? 0 : req.getSort());
    }

    private HrDepartmentDO requireDept(Long id) {
        HrDepartmentDO dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BizException(BizCode.DATA_NOT_FOUND);
        }
        return dept;
    }

    private DeptResp toResp(HrDepartmentDO dept) {
        return DeptResp.builder()
                .id(dept.getId())
                .parentId(dept.getParentId())
                .deptName(dept.getDeptName())
                .deptCode(dept.getDeptCode())
                .sort(dept.getSort())
                .status(dept.getStatus())
                .createTime(dept.getCreateTime())
                .build();
    }
}
