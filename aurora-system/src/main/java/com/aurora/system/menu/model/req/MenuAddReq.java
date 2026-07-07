package com.aurora.system.menu.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 菜单新增请求。
 */
@Data
public class MenuAddReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long parentId = 0L;

    @NotBlank(message = "菜单标题不能为空")
    private String title;

    @NotNull(message = "菜单类型不能为空")
    private Integer type;

    private String name;
    private String path;
    private String component;
    private String icon;
    private String permission;
    private Integer sort = 0;
    private Integer visible = 1;
    private Integer status = 1;
}
