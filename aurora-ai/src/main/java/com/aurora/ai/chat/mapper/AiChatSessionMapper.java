package com.aurora.ai.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.aurora.ai.chat.entity.AiChatSessionDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiChatSessionMapper extends BaseMapper<AiChatSessionDO> {
}