package com.aurora.ai.runtime.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.aurora.ai.runtime.entity.AiAgentTaskDO;
import org.apache.ibatis.annotations.Mapper;

/** Agent 任务数据访问接口。 */
@Mapper
public interface AiAgentTaskMapper extends BaseMapper<AiAgentTaskDO> {
}
