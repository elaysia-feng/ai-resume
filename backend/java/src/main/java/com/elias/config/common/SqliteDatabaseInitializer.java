package com.elias.config.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite 数据目录与建表脚本的初始化。
 *
 * <p>为什么不能放在 ApplicationRunner 里：</p>
 * <ol>
 *   <li>Spring Boot 在 DataSource 就绪后会自动初始化数据源，这发生在 ApplicationRunner 之前；
 *       而 xerial 驱动连不上时直接抛 "path does not exist"，不会自己建父目录。</li>
 *   <li>更关键：{@code AgentResultConsumer} 的 @PostConstruct 会往 agent_stream_group 插记录，
 *       它在所有 bean 初始化阶段就跑完了 —— 等 ApplicationRunner 执行时表还不存在，
 *       bean 创建直接失败，后端起不来。</li>
 * </ol>
 *
 * <p>所以这里挂成 BeanPostProcessor，在 DataSource 这个 bean 初始化完成的最后一步：
 * 建目录 + 执行建表脚本。这早于任何依赖 DataSource 的业务 bean，时序上够安全。</p>
 *
 * <p>建表脚本打进 jar 的 db_schema/schema.sql（见 pom.xml 的 resources 配置），
 * 负责本地测试版本的表和索引初始化；正式发行时再引入版本化迁移。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SqliteDatabaseInitializer implements BeanPostProcessor {

    /** 建表脚本在 jar 内的位置 */
    private static final String SCHEMA_RESOURCE = "db_schema/schema.sql";

    private final Environment environment;

    /**
     * 初始化 DataSource 后补齐本地 SQLite 表结构。
     *
     * @param bean 初始化完成的 Spring Bean
     * @param beanName Bean 名称
     * @return 继续交给容器使用的 Bean
     * @throws BeansException Bean 初始化异常
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource dataSource) {
            String url = environment.getProperty("spring.datasource.url", "");
            ensureParentDirectory(url);
            applySchema(dataSource);
        }
        return bean;
    }

    /**
     * 从 JDBC URL 里取出文件路径并确保父目录存在。
     *
     * 1. 移除 SQLite URL 前缀和查询参数。
     * 2. 跳过内存数据库，并检查文件父目录。
     * 3. 创建缺失目录并在失败时保留底层异常。
     *
     * @param url SQLite 数据库连接地址
     */
    private void ensureParentDirectory(String url) {
        // 1. 过滤非 SQLite 地址，取出数据库文件路径。
        String prefix = "jdbc:sqlite:";
        if (url == null || !url.startsWith(prefix)) {
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
        // 2. 定位数据库文件的父目录，已有目录时直接返回。
        Path path = Paths.get(filePart).toAbsolutePath();
        Path parent = path.getParent();
        if (parent == null || Files.isDirectory(parent)) {
            return;
        }
        try {
            // 3. 创建缺少的目录；失败时保留异常原因供启动日志定位。
            Files.createDirectories(parent);
            log.info("已创建 SQLite 数据目录: {}", parent);
        } catch (IOException e) {
            throw new IllegalStateException("创建 SQLite 数据目录失败: " + parent, e);
        }
    }

    /**
     * 执行建表脚本。脚本幂等，重复执行不会报错。
     *
     * 1. 检查脚本资源和现有表结构。
     * 2. 为旧会话表补加可空分区字段。
     * 3. 执行初始化脚本并转换数据库异常。
     *
     * @param dataSource SQLite 数据源
     */
    private void applySchema(DataSource dataSource) {
        // 1. 脚本缺失时跳过初始化，避免阻断后续容器启动。
        ClassPathResource resource = new ClassPathResource(SCHEMA_RESOURCE);
        if (!resource.exists()) {
            log.warn("未找到建表脚本 {}，跳过自动建表", SCHEMA_RESOURCE);
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            // 2. 校验旧表，补加可空字段，再执行幂等建表脚本。
            validateLocalSchema(connection);
            ensureAgentSessionTargetSectionColumn(connection);
            ScriptUtils.executeSqlScript(connection, resource);
            log.info("建表脚本执行完成: {}", SCHEMA_RESOURCE);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("执行建表脚本失败: " + SCHEMA_RESOURCE, e);
        }
    }

    /**
     * 添加可空的会话分区字段，保留已有的本地简历和 AI 会话数据。
     *
     * @param connection 当前 SQLite 连接
     */
    private void ensureAgentSessionTargetSectionColumn(Connection connection) throws SQLException {
        if (!tableExists(connection, "ai_session") || hasColumn(connection, "ai_session", "target_section_id")) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE ai_session ADD COLUMN target_section_id BIGINT NULL");
        }
    }

    /**
     * 只自动补加无损的可空字段；其它不兼容的本地测试表仍提示重置数据库。
     *
     * @param connection 当前 SQLite 连接
     */
    private void validateLocalSchema(Connection connection) throws SQLException {
        if (tableExists(connection, "ai_interview_round")
                && (hasColumn(connection, "ai_interview_round", "user_id")
                        || hasColumn(connection, "ai_interview_round", "session_id")
                        || hasColumn(connection, "ai_interview_round", "resume_id"))) {
            throw outdatedLocalDatabase();
        }
        if (tableExists(connection, "ai_agent_run")
                && (!isNotNull(connection, "ai_agent_run", "target_section_id")
                        || !isNotNull(connection, "ai_agent_run", "active_flag"))) {
            throw outdatedLocalDatabase();
        }
    }

    private IllegalStateException outdatedLocalDatabase() {
        return new IllegalStateException(
                "检测到不兼容的旧版本地 SQLite 表结构；除可空会话分区字段外不执行迁移，请清空 DB_FILE 指向的测试数据库后重启");
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?")) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean hasColumn(Connection connection, String tableName, String columnName) throws SQLException {
        // 1. 读取 SQLite 表结构元数据。
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            // 2. 按列名匹配目标字段。
            while (resultSet.next()) {
                if (columnName.equals(resultSet.getString("name"))) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean isNotNull(Connection connection, String tableName, String columnName) throws SQLException {
        // 1. 读取 SQLite 表结构元数据。
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            // 2. 按列名匹配目标字段并返回非空约束。
            while (resultSet.next()) {
                if (columnName.equals(resultSet.getString("name"))) {
                    return resultSet.getInt("notnull") == 1;
                }
            }
            return false;
        }
    }
}
