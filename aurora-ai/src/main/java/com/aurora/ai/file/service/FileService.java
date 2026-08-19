package com.aurora.ai.file.service;

import com.aurora.ai.file.model.req.FilePageReq;
import com.aurora.ai.file.model.resp.FileResp;
import com.aurora.common.response.PageResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件管理服务（参考 ContiNew Admin 文件管理：上传/列表/删除）
 */
public interface FileService {

    /** 上传文件：保存到本地 + 记录 sys_file */
    FileResp upload(MultipartFile file);

    /** 分页查询文件 */
    PageResult<FileResp> page(FilePageReq req);

    /** 删除文件（逻辑删记录 + 尝试删除磁盘文件） */
    void delete(Long id);
}
