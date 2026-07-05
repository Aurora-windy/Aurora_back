package com.aurora.common.constant;

/**
 * 业务通用常量
 */
public final class BizConst {

    private BizConst() {
    }

    /** 超级管理员角色编码 */
    public static final String ROLE_ADMIN = "admin";

    /** 默认密码（重置密码时用） */
    public static final String DEFAULT_PASSWORD = "Aurora@123";

    /** 文件上传单文件大小上限：20 MB（SRS §4.1） */
    public static final long MAX_UPLOAD_SIZE = 20L * 1024 * 1024;

    /** JWT Token 默认有效期：2 小时（秒） */
    public static final long JWT_EXPIRE_SECONDS = 7200L;

    /** Refresh Token 默认有效期：7 天（秒） */
    public static final long JWT_REFRESH_EXPIRE_SECONDS = 7 * 24 * 3600L;

    /** 电商订单超时取消时间：15 分钟（秒，Phase 5） */
    public static final long ORDER_TIMEOUT_SECONDS = 15 * 60L;

    /** BCrypt 加密强度（4-31，10 是性能与安全的平衡点） */
    public static final int BCRYPT_STRENGTH = 10;

    /** 文件上传允许的扩展名白名单（spec §4.2.1）：图片 / 文档 / 代码 */
    public static final java.util.Set<String> FILE_ALLOWED_EXT = java.util.Set.of(
            "jpg", "jpeg", "png", "gif", "webp",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md",
            "java", "py", "go", "js", "ts", "c", "cpp"
    );
}
