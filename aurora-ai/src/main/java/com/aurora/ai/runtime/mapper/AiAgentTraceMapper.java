package com.aurora.ai.runtime.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.aurora.ai.runtime.entity.AiAgentTraceDO;
import org.apache.ibatis.annotations.Mapper;

/** Agent Trace 数据访问接口。 */
@Mapper
public interface AiAgentTraceMapper extends BaseMapper<AiAgentTraceDO> {
}
