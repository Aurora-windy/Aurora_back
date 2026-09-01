package com.aurora.ai.mcp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.aurora.ai.mcp.entity.AiMcpServerDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MCP Server 配置 Mapper（T-M2）。
 */
@Mapper
public interface AiMcpServerMapper extends BaseMapper<AiMcpServerDO> {
}
