package com.aurora.ai.runtime.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.aurora.ai.runtime.entity.AiAgentRuntimeEventDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** Agent 运行时事件数据访问接口。 */
@Mapper
public interface AiAgentRuntimeEventMapper extends BaseMapper<AiAgentRuntimeEventDO> {

    /** 查询任务当前最大的事件序号，用于生成可回放的连续事件。 */
    @Select("SELECT COALESCE(MAX(`sequence`), 0) FROM ai_agent_runtime_event WHERE task_id = #{taskId} AND deleted = 0")
    Long selectMaxSequence(@Param("taskId") Long taskId);
}
