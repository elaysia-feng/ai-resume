package com.airesumeforge;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.airesumeforge.mapper")
public class AiResumeForgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiResumeForgeApplication.class, args);
    }
}
