package com.aurora.ai.file.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aurora.common.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件管理（参考 ContiNew Admin FileMain：上传/列表/删除，文件存本地 /uploads/**）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_file")
public class SysFileDO extends BaseDO {
    /** 原文件名 */
    private String name;
    /** 访问地址（/uploads/**） */
    private String url;
    /** 存储相对路径 */
    private String relativePath;
    /** 扩展名 */
    private String fileType;
    /** 文件大小（字节） */
    private Long size;
}
