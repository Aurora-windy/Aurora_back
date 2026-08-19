package com.aurora.ai.knowledge.model.resp;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
public class KnowledgeDocResp implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String type;
    /** 原文件访问地址（本地文件上传保存后） */
    private String fileUrl;
    private String status;
    private String content;
    private String summary;
    private Integer version;
    private LocalDateTime publishedAt;
    private LocalDateTime createTime;
}