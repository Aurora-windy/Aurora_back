package com.aurora.system.menu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统菜单 DO。
 *
 * <p>当前 sys_menu 表没有 create_user/update_user 字段，因此不继承 BaseDO。</p>
 */
@Data
@TableName("sys_menu")
public class SysMenuDO {

    @TableId(type = IdType.AUTO)
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

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
