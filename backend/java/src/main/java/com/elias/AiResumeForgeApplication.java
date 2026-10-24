package com.elias;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI Resume Forge Java 后端单体应用启动类。
 *
 * <p>原先拆成 gateway + auth-service + resume-service + agent-service + interview-service
 * + order + payment + notification 八个进程，这里合并为一个 Spring Boot 应用：
 * 所有领域包（auth / resume / agent / interview / notification / common）
 * 都在同一个进程内按包隔离，跨领域调用直接走 Service 或 Mapper，不再经过 HTTP。</p>
 *
 * <p>支付功能移除后，order / payment 两个领域包已删除，对应的 Mapper 扫描配置也一并去掉。</p>
 */
@SpringBootApplication(scanBasePackages = "com.elias")
@MapperScan({
        "com.elias.auth.mapper",
        "com.elias.resume.mapper",
        "com.elias.agent.mapper",
        "com.elias.interview.mapper",
        "com.elias.notification.mapper"
})
public class AiResumeForgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiResumeForgeApplication.class, args);
    }
}
