package com.aurora.common.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分页请求基类。
 *
 * <p>spec: docs/specs/2026-07-05-interface-baseline.md §5.1</p>
 */
@Data
@Schema(description = "分页请求")
public class PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "页码，从1开始", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数，最大100", example = "10")
    private Integer pageSize = 10;

    @Schema(description = "排序字段（驼峰）", example = "createTime")
    private String orderField;

    @Schema(description = "排序方向：asc/desc", example = "desc")
    private String orderDirection = "desc";

    public long normalizedPageNum() {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    public long normalizedPageSize() {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }
}
