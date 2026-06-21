package com.aurora.aurora_back;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan("com.aurora.aurora_back.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class AuroraBackApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuroraBackApplication.class, args);
    }

}
