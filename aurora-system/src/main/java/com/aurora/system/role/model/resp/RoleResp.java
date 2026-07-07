package com.aurora.system.role.model.resp;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色响应。
 */
@Data
@Builder
public class RoleResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String code;
    private Integer dataScope;
    private Integer sort;
    private Integer status;
    private String remark;
    private List<Long> menuIds;
    private LocalDateTime createTime;
}
