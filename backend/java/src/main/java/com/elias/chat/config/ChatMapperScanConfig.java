package com.elias.chat.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * Chat 模块的 Mapper 扫描配置。
 *
 * <p>主启动类的 {@code @MapperScan} 没有覆盖 com.elias.chat，这里单独声明一份，
 * 保证 chat 包的 Mapper 能被 MyBatis 注册。</p>
 */
@Configuration
@MapperScan("com.elias.chat.mapper")
public class ChatMapperScanConfig {
}
