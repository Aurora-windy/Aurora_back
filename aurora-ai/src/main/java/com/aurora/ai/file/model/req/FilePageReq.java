package com.aurora.ai.file.model.req;

import lombok.Data;

/**
 * 文件分页查询
 */
@Data
public class FilePageReq {
    private Integer pageNum = 1;
    private Integer pageSize = 24;
    /** 文件名模糊搜索 */
    private String name;

    public int normalizedPageNum() {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    public int normalizedPageSize() {
        return pageSize == null || pageSize < 1 ? 24 : Math.min(pageSize, 200);
    }
}
