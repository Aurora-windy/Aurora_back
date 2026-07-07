package com.aurora.system.menu.model.resp;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 当前用户前端路由响应。
 */
@Data
@Builder
public class UserRouteResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long parentId;
    private String title;
    private String name;
    private String path;
    private String component;
    private String icon;
    private Integer type;
    private Integer sort;

    @Builder.Default
    private List<UserRouteResp> children = new ArrayList<>();
}
