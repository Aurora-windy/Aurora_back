package com.aurora.hr.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.aurora.hr.attendance.entity.HrAttendanceDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HrAttendanceMapper extends BaseMapper<HrAttendanceDO> {
}
