package com.aurora.hr.employee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.aurora.hr.employee.entity.HrEmployeeDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HrEmployeeMapper extends BaseMapper<HrEmployeeDO> {
}
