package com.aurora.mall.order.model.req;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OrderCreateReq {
    private List<Long> cartIds = new ArrayList<>();
}
