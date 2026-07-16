package com.aurora.ai.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aurora.common.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_knowledge_doc")
public class AiKnowledgeDocDO extends BaseDO {
    private String title;
    private String type;
    private String status;
    private String content;
    private String summary;
    private Integer version;
    private LocalDateTime publishedAt;
}