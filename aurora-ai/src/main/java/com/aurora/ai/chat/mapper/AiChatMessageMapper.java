package com.aurora.ai.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.aurora.ai.chat.entity.AiChatMessageDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiChatMessageMapper extends BaseMapper<AiChatMessageDO> {
}