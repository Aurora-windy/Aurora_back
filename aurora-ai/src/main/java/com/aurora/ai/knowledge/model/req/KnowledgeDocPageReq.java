package com.aurora.ai.knowledge.model.req;

import com.aurora.common.base.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class KnowledgeDocPageReq extends PageRequest {
    private String title;
    private String type;
    private String status;
}