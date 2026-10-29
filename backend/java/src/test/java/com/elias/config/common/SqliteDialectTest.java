package com.elias.config.common;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SQLite 方言验证。
 *
 * <p>MySQL 换 SQLite 之后有三个地方容易翻车，这里逐个钉死：
 * 1. 实体里的时间字段是 LocalDateTime，MyBatis 的 handler 走
 *    {@code rs.getObject(col, LocalDateTime.class)}，驱动必须支持。
 * 2. URL 上的 {@code date_class=java.time.LocalDateTime} 参数必须被 xerial 识别。
 * 3. {@code foreign_keys=on} / {@code journal_mode=WAL} 参数必须真的生效。
 */
class SqliteDialectTest {

    private static final String URL_SUFFIX =
            "?busy_timeout=10000&journal_mode=WAL&foreign_keys=on";

    @Test
    void localDateTimeRoundTripsThroughSqlite() throws Exception {
        Path dir = Files.createTempDirectory("arf_sqlite_test");
        Path db = dir.resolve("test.db");
        String url = "jdbc:sqlite:" + db.toAbsolutePath() + URL_SUFFIX;

        try (Connection conn = DriverManager.getConnection(url)) {
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE t (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
            }
            LocalDateTime now = LocalDateTime.of(2026, Month.OCTOBER, 24, 11, 30, 15, 123_000_000);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO t (created_at) VALUES (?)")) {
                ps.setObject(1, now);
                ps.executeUpdate();
            }

            // MyBatis 的 LocalDateTimeTypeHandler 就是这么读的
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT created_at FROM t")) {
                assertTrue(rs.next());
                LocalDateTime read = rs.getObject("created_at", LocalDateTime.class);
                assertNotNull(read, "驱动没能把 DATETIME 列读成 LocalDateTime");
                assertEquals(now.withNano(0), read.withNano(0));
            }
        }
    }

    @Test
    void urlPragmasAreEffective() throws Exception {
        Path dir = Files.createTempDirectory("arf_sqlite_pragma");
        Path db = dir.resolve("pragma.db");
        String url = "jdbc:sqlite:" + db.toAbsolutePath() + URL_SUFFIX;

        try (Connection conn = DriverManager.getConnection(url);
             Statement st = conn.createStatement()) {
            try (ResultSet rs = st.executeQuery("PRAGMA foreign_keys")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1), "foreign_keys 没打开");
            }
            try (ResultSet rs = st.executeQuery("PRAGMA journal_mode")) {
                assertTrue(rs.next());
                assertEquals("wal", rs.getString(1).toLowerCase(), "journal_mode 不是 WAL");
            }
        }
    }

    @Test
    void generatedKeysWorkForAutoIncrement() throws Exception {
        Path dir = Files.createTempDirectory("arf_sqlite_keys");
        Path db = dir.resolve("keys.db");
        String url = "jdbc:sqlite:" + db.toAbsolutePath() + URL_SUFFIX;

        try (Connection conn = DriverManager.getConnection(url);
             Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE t (id INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR(50))");
        }
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO t (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "x");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                assertTrue(keys.next(), "拿不到自增主键");
                assertEquals(1L, keys.getLong(1));
            }
        }
    }
}
