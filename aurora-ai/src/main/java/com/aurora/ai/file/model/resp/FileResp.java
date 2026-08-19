package com.aurora.ai.file.model.resp;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件信息（上传/列表响应）
 */
@Data
@Builder
public class FileResp implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    /** 原文件名 */
    private String name;
    /** 访问地址 */
    private String url;
    /** 扩展名 */
    private String fileType;
    /** 文件大小（字节） */
    private Long size;
    private LocalDateTime createTime;
}
