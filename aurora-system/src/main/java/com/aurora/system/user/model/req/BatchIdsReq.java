package com.aurora.system.user.model.req;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 批量 ID 请求。
 */
@Data
public class BatchIdsReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "ID 列表不能为空")
    private List<Long> ids;
}
