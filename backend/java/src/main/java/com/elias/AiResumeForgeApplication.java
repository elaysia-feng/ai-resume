package com.elias;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI Resume Forge Java 后端单体应用启动类。
 *
 * <p>认证、简历、Agent、面试、聊天和通知功能运行在同一个 Spring Boot 进程中。
 * HTTP、DTO、Service、Mapper、Entity 和配置按统一分层组织，并在层内保留领域子包。</p>
 *
 * <p>支付功能移除后，order / payment 两个领域包已删除，对应的 Mapper 扫描配置也一并去掉。</p>
 */
@SpringBootApplication(scanBasePackages = "com.elias")
@MapperScan({
        "com.elias.mapper.auth",
        "com.elias.mapper.resume",
        "com.elias.mapper.agent",
        "com.elias.mapper.interview",
        "com.elias.mapper.notification"
})
public class AiResumeForgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiResumeForgeApplication.class, args);
    }
}
