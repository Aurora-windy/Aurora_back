package com.aurora.system.menu.model.resp;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树响应。
 */
@Data
@Builder
public class MenuTreeResp implements Serializable {

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

    @Builder.Default
    private List<MenuTreeResp> children = new ArrayList<>();
}
