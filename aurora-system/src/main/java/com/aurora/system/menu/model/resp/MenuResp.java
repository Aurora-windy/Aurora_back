package com.aurora.system.menu.model.resp;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 菜单响应。
 */
@Data
@Builder
public class MenuResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long parentId;
    private String title;
    private Integer type;
    private String name;
    private String path;
    private String component;
    private String icon;
    private String permission;
    private Integer sort;
    private Integer visible;
    private Integer status;
    private LocalDateTime createTime;
}
