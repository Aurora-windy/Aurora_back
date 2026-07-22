package com.aurora.mall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.aurora.mall.product.entity.MallProductDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MallProductMapper extends BaseMapper<MallProductDO> {

    @Update("""
            UPDATE mall_product
               SET stock = stock - #{quantity},
                   update_time = NOW()
             WHERE id = #{productId}
               AND stock >= #{quantity}
               AND status = 1
               AND deleted = 0
            """)
    int deductStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
