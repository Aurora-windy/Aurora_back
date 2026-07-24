package com.aurora.mall.seckill.model.resp;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class StockLogResp {
    private Long id;
    private Long productId;
    private String productName;
    private Integer bizType;
    private Integer quantity;
    private Long orderId;
    private String remark;
    private LocalDateTime createTime;
}
