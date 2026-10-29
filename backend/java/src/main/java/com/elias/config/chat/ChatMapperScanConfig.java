package com.elias.config.chat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * Chat 模块的 Mapper 扫描配置。
 *
 * <p>主启动类的扫描列表未覆盖聊天域 Mapper，因此在此声明对应的 Mapper 包。</p>
 */
@Configuration
@MapperScan("com.elias.mapper.chat")
public class ChatMapperScanConfig {
}
