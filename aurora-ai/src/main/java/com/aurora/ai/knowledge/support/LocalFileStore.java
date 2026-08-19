package com.aurora.ai.knowledge.support;

import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * 本地文件存储（参考 ContiNew Admin 本地存储方案，零第三方依赖）。
 * <p>上传文件按 {@code yyyy/MM/dd/uuid.ext} 保存到 {@code aurora.upload.path}（默认 ./uploads），
 * 通过静态资源映射 {@code /uploads/**} 提供访问 URL。</p>
 */
@Slf4j
@Component
public class LocalFileStore {

    private static final String URL_PREFIX = "/uploads/";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final Path root;

    public LocalFileStore(@Value("${aurora.upload.path:./uploads}") String uploadPath) {
        this.root = Paths.get(uploadPath).toAbsolutePath().normalize();
    }

    /** 保存上传文件，返回可访问 URL 等元信息 */
    public FileRef store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(BizCode.PARAM_ERROR, "上传文件不能为空");
        }
        String originalName = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename() : "file";
        String ext = extName(originalName);
        String datePath = LocalDate.now().format(DATE_FORMAT);
        String fileName = UUID.randomUUID().toString().replace("-", "")
                + (ext.isEmpty() ? "" : "." + ext);
        Path dir = root.resolve(datePath);
        Path target = dir.resolve(fileName).normalize();
        if (!target.startsWith(root)) {
            throw new BizException(BizCode.PARAM_ERROR, "非法文件路径");
        }
        try {
            Files.createDirectories(dir);
            file.transferTo(target);
        } catch (IOException e) {
            log.error("本地文件保存失败: {}", e.getMessage(), e);
            throw new BizException(BizCode.DOC_PROCESSING_FAILED, "文件保存失败：" + e.getMessage());
        }
        String relativePath = datePath + "/" + fileName;
        log.info("本地文件已保存: {} ({})", relativePath, file.getSize());
        return new FileRef(URL_PREFIX + relativePath, relativePath, originalName, file.getSize());
    }

    /** 删除已保存的文件（按相对路径），文件不存在时静默忽略 */
    public void delete(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            return;
        }
        try {
            Path target = root.resolve(relativePath).normalize();
            if (target.startsWith(root)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException e) {
            log.warn("删除本地文件失败: {} ({})", relativePath, e.getMessage());
        }
    }

    private String extName(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        int idx = lower.lastIndexOf('.');
        if (idx < 0 || idx == lower.length() - 1) {
            return "";
        }
        return lower.substring(idx + 1);
    }

    /** 文件引用信息 */
    public record FileRef(String url, String relativePath, String originalName, long size) {
    }
}
