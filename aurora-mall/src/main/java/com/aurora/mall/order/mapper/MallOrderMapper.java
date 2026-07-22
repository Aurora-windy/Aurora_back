package com.aurora.mall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.aurora.mall.order.entity.MallOrderDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MallOrderMapper extends BaseMapper<MallOrderDO> {
}
