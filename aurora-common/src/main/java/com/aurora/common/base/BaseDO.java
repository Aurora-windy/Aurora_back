package com.aurora.common.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类
 *
 * <p>所有 DO 继承此基类，统一管理 ID、审计字段、逻辑删除字段。
 * 字段自动填充由 {@link com.aurora.common.handler.MetaObjectHandler} 处理。</p>
 *
 * <p>对应数据库列约定：</p>
 * <ul>
 *   <li>id           - bigint, 主键, 雪花算法生成</li>
 *   <li>create_user  - bigint, 创建人 user_id</li>
 *   <li>create_time  - datetime, 创建时间</li>
 *   <li>update_user  - bigint, 修改人 user_id</li>
 *   <li>update_time  - datetime, 修改时间</li>
 *   <li>deleted      - tinyint, 逻辑删除: 0未删 1已删</li>
 * </ul>
 */
@Data
public class BaseDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private Long updateUser;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
