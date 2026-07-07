package com.aurora.system.menu.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 菜单修改请求。
 */
@Data
public class MenuUpdateReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "菜单 ID 不能为空")
    private Long id;

    private Long parentId;

    @NotBlank(message = "菜单标题不能为空")
    private String title;

    @NotNull(message = "菜单类型不能为空")
    private Integer type;

    private String name;
    private String path;
    private String component;
    private String icon;
    private String permission;
    private Integer sort;
    private Integer visible;
    private Integer status;
}
