package com.aurora.oj.problem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.aurora.oj.problem.entity.OjProblemDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OjProblemMapper extends BaseMapper<OjProblemDO> {
}
