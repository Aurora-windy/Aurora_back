package com.aurora.ai.file.service.impl;

import com.aurora.ai.file.entity.SysFileDO;
import com.aurora.ai.file.mapper.SysFileMapper;
import com.aurora.ai.file.model.req.FilePageReq;
import com.aurora.ai.file.model.resp.FileResp;
import com.aurora.ai.file.service.FileService;
import com.aurora.ai.knowledge.support.LocalFileStore;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.aurora.common.response.PageResult;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final SysFileMapper fileMapper;
    private final LocalFileStore localFileStore;

    @Override
    public FileResp upload(MultipartFile file) {
        LocalFileStore.FileRef ref = localFileStore.store(file);
        SysFileDO entity = new SysFileDO();
        entity.setName(ref.originalName());
        entity.setUrl(ref.url());
        entity.setRelativePath(ref.relativePath());
        entity.setFileType(extName(ref.originalName()));
        entity.setSize(ref.size());
        fileMapper.insert(entity);
        return toResp(entity);
    }

    @Override
    public PageResult<FileResp> page(FilePageReq req) {
        Page<SysFileDO> page = fileMapper.selectPage(new Page<>(req.normalizedPageNum(), req.normalizedPageSize()),
                Wrappers.<SysFileDO>lambdaQuery()
                        .like(StringUtils.hasText(req.getName()), SysFileDO::getName, req.getName())
                        .orderByDesc(SysFileDO::getCreateTime));
        return new PageResult<>(page.getRecords().stream().map(this::toResp).toList(), page.getTotal());
    }

    @Override
    public void delete(Long id) {
        SysFileDO entity = fileMapper.selectById(id);
        if (entity == null) {
            throw new BizException(BizCode.DATA_NOT_FOUND, "文件不存在");
        }
        fileMapper.deleteById(id);
        // 删除磁盘文件（失败仅告警，不影响记录删除）
        localFileStore.delete(entity.getRelativePath());
    }

    private String extName(String name) {
        if (name == null) {
            return "";
        }
        int idx = name.lastIndexOf('.');
        return idx >= 0 && idx < name.length() - 1 ? name.substring(idx + 1).toLowerCase() : "";
    }

    private FileResp toResp(SysFileDO entity) {
        return FileResp.builder()
                .id(entity.getId())
                .name(entity.getName())
                .url(entity.getUrl())
                .fileType(entity.getFileType())
                .size(entity.getSize())
                .createTime(entity.getCreateTime())
                .build();
    }
}
