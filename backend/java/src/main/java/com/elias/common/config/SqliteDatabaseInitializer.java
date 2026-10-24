package com.elias.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 启动时确保 SQLite 数据文件所在目录存在。
 *
 * <p>SQLite 不会像 MySQL 那样自建库目录，文件路径的父目录不存在时驱动会直接报
 * "unable to open database file"。这里在连接池建连之前把目录建出来。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SqliteDatabaseInitializer implements ApplicationRunner {

    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        String url = environment.getProperty("spring.datasource.url", "");
        String prefix = "jdbc:sqlite:";
        if (!url.startsWith(prefix)) {
            return;
        }
        String filePart = url.substring(prefix.length());
        int queryIndex = filePart.indexOf('?');
        if (queryIndex >= 0) {
            filePart = filePart.substring(0, queryIndex);
        }
        if (filePart.isEmpty() || filePart.startsWith(":memory:")) {
            return;
        }
        Path path = Paths.get(filePart).toAbsolutePath();
        Path parent = path.getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            throw new IllegalStateException("创建 SQLite 数据目录失败: " + parent, e);
        }
        log.info("SQLite 数据库文件: {}", path);
    }
}
