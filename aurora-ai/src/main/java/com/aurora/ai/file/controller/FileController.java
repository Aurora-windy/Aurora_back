package com.aurora.ai.file.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.ai.file.model.req.FilePageReq;
import com.aurora.ai.file.model.resp.FileResp;
import com.aurora.ai.file.service.FileService;
import com.aurora.common.constant.PermCodeConst;
import com.aurora.common.response.PageResult;
import com.aurora.common.response.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件管理 API（参考 ContiNew Admin FileController）
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/system/file")
public class FileController {

    private final FileService fileService;

    /** 上传文件（保存到本地 /uploads/**，记录 sys_file） */
    @SaCheckPermission(PermCodeConst.System.File.UPLOAD)
    @PostMapping("/upload")
    public Result<FileResp> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(fileService.upload(file));
    }

    /** 分页查询文件 */
    @SaCheckPermission(PermCodeConst.System.File.LIST)
    @GetMapping
    public Result<PageResult<FileResp>> page(FilePageReq req) {
        return Result.ok(fileService.page(req));
    }

    /** 删除文件 */
    @SaCheckPermission(PermCodeConst.System.File.DELETE)
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        fileService.delete(id);
        return Result.ok(Boolean.TRUE);
    }
}
