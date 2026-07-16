package com.aurora.ai.provider.model.req;

import com.aurora.common.base.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProviderPageReq extends PageRequest {
    private String code;
    private String name;
    private String model;
    private Integer enabled;
}