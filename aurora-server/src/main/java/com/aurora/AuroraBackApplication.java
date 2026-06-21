package com.aurora;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * AURORA 综合一体化后台平台 启动类
 *
 * <p>聚合所有业务模块（system/hr/edu/oj/mall/ai），由 {@code aurora-server} 模块统一启动。</p>
 */
@SpringBootApplication
@MapperScan("com.aurora.**.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class AuroraBackApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuroraBackApplication.class, args);
    }
}
