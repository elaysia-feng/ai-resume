package com.elias.common.stream;

import com.elias.agent.dto.run.request.AgentRunJobMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Java ↔ Python 跨语言互读验证（SQLite 任务队列）。
 *
 * <p>需要先跑 {@code bench/verify_stream_e2e.py publish} 让 Python 侧投一条标记消息，
 * 再带上 {@code -Dstream.cross.check=true} 跑这个测试类。</p>
 *
 * <p>用系统属性门控，是为了让普通离线构建仍然全绿（没 Python 预跑时直接跳过）。</p>
 */
@EnabledIfSystemProperty(named = "stream.cross.check", matches = "true")
class AgentStreamCrossLanguageTest {

    /**
     * Python 侧投递任务用的流。
     */
    private static final String RESUME_STREAM = "agent.resume.run";

    /**
     * Python 侧 worker 的消费组名。
     */
    private static final String RESUME_GROUP = "agent-resume-worker";

    /**
     * Python 侧留下的标记 runId。
     */
    private static final int PYTHON_MARKER_RUN_ID = 999001;

    /**
     * Java 侧投给 Python 读的流。
     */
    private static final String CROSS_RESULT_STREAM = "test.cross.java.result";

    private static final String CROSS_RESULT_GROUP = "python-cross-reader";

    private static JdbcTemplate jdbcTemplate;
    private static AgentStreamClient client;

    @BeforeAll
    static void setUp() throws Exception {
        // 两端必须指向同一个 SQLite 文件，路径由 CROSS_TEST_DB 环境变量（或同名系统属性）给出
        String dbPath = System.getProperty("stream.cross.db", System.getenv("CROSS_TEST_DB"));
        assumeTrue(dbPath != null && !dbPath.isBlank(),
                "没设置 CROSS_TEST_DB，跳过跨语言验证（先跑 bench/verify_stream_e2e.py publish）");
        Path dbFile = Paths.get(dbPath);
        if (dbFile.getParent() != null) {
            Files.createDirectories(dbFile.getParent());
        }
        String url = "jdbc:sqlite:" + dbFile.toAbsolutePath()
                + "?busy_timeout=10000&journal_mode=WAL&foreign_keys=on";
        DataSource dataSource = new DriverManagerDataSource(url);
        jdbcTemplate = new JdbcTemplate(dataSource);
        try (Connection conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new FileSystemResource("db_schema/schema.sql"));
        }
        client = new AgentStreamClient(jdbcTemplate, new ObjectMapper(), new AgentStreamProperties());
    }

    @AfterAll
    static void tearDown() {
        // 临时目录由操作系统清理
    }

    @Test
    void javaCanReadMessagePublishedByPython() throws Exception {
        client.ensureGroup(RESUME_STREAM, RESUME_GROUP);
        List<AgentStreamClient.StreamMessage> messages =
                client.read(RESUME_STREAM, RESUME_GROUP, "java-cross-reader", 100);

        AgentStreamClient.StreamMessage marker = null;
        ObjectMapper objectMapper = new ObjectMapper();
        for (AgentStreamClient.StreamMessage message : messages) {
            String payload = message.payload();
            if (payload != null) {
                // 不要用字符串 contains 比字段：Python 的 json.dumps 默认带空格，
                // 和 Java 的序列化结果文本不一致，但 JSON 语义是一样的。
                Map<String, Object> parsed = objectMapper.readValue(payload, Map.class);
                Object runId = parsed.get("runId");
                if (runId != null && Integer.valueOf(PYTHON_MARKER_RUN_ID).equals(((Number) runId).intValue())) {
                    marker = message;
                }
            }
            client.acknowledge(RESUME_STREAM, RESUME_GROUP, message.id());
        }

        assertNotNull(marker, "没读到 Python 投的标记消息 runId=" + PYTHON_MARKER_RUN_ID);
        // Python 投的这条必须能被 Java 的 DTO 反序列化出来
        AgentRunJobMessage job = new ObjectMapper().readValue(marker.payload(), AgentRunJobMessage.class);
        assertEquals(PYTHON_MARKER_RUN_ID, job.getRunId().intValue());
        assertEquals("START", job.getJobType());
        assertEquals("JD_CUSTOMIZE", job.getSceneCode());
    }

    @Test
    void javaPublishesRealDtoForPythonToRead() {
        // Python 侧 verify_stream_e2e.py consume 会来读这条
        client.ensureGroup(CROSS_RESULT_STREAM, CROSS_RESULT_GROUP);

        AgentRunJobMessage message = AgentRunJobMessage.builder()
                .jobType("START")
                .runId(4242L)
                .sessionId(11L)
                .resumeId(22L)
                .sceneCode("JD_CUSTOMIZE")
                .targetSectionId(33L)
                .userInput("java -> python 标记")
                .jobDescription("Java 后端工程师")
                .build();
        Long messageId = client.publish(CROSS_RESULT_STREAM, CROSS_RESULT_GROUP, message);
        assertNotNull(messageId);
    }
}
