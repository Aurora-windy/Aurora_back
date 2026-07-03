package com.aurora.common.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 临时工具：生成 Liquibase seed 中 admin 账号的 BCrypt 密码 hash。
 * hash 复制到 sys_data.sql 后此测试可删除或保留作 PasswordEncoder 验证用。
 */
class BCryptGenTest {

    @Test
    void generateAdminPasswordHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("Aurora@123");
        System.out.println("BCRYPT_HASH=" + hash);
        System.out.println("VERIFY=" + encoder.matches("Aurora@123", hash));
    }
}
