package com.aurora.mall.seckill.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SeckillJoinReq {
    @NotNull private Long activityId;
    @NotBlank private String idempotentId;
}
