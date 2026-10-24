package com.elias.common.stream;

import com.elias.agent.dto.run.request.AgentRunJobMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Agent 任务队列（SQLite）端到端验证。
 *
 * <p>用的是真实的 schema.sql 和真实的 SQLite 驱动，
 * 所以它同时验证了「建表脚本能跑」和「队列语义对」。</p>
 *
 * <p>另外锁死了 Java -&gt; Python 的字段名契约：
 * Python 侧用 pydantic alias 收这些字段，字段名一变 Python 就会静默丢消息。</p>
 */
class AgentStreamClientIntegrationTest {

    private static JdbcTemplate jdbcTemplate;
    private static AgentStreamClient client;
    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setUp() throws Exception {
        Path dir = Files.createTempDirectory("arf_stream_test");
        Path dbFile = dir.resolve("ai_resume.db");
        String url = "jdbc:sqlite:" + dbFile.toAbsolutePath()
                + "?busy_timeout=10000&journal_mode=WAL&foreign_keys=on";
        DataSource dataSource = new DriverManagerDataSource(url);
        jdbcTemplate = new JdbcTemplate(dataSource);

        // 用真实的建表脚本，顺带验证 schema.sql 能干净执行
        try (Connection conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new FileSystemResource("db_schema/schema.sql"));
        }

        objectMapper = new ObjectMapper();
        client = new AgentStreamClient(jdbcTemplate, objectMapper, new AgentStreamProperties());
    }

    @AfterAll
    static void tearDown() {
        // 临时目录由操作系统清理
    }

    @Test
    void publishThenReadBackInSameGroup() {
        String stream = "test.stream.basic";
        String group = "test-group-basic";
        client.ensureGroup(stream, group);

        Long messageId = client.publish(stream, group, Map.of("hello", "world"));
        assertNotNull(messageId);

        List<AgentStreamClient.StreamMessage> messages =
                client.read(stream, group, "consumer-1", 10);
        assertEquals(1, messages.size());
        assertEquals("{\"hello\":\"world\"}", messages.get(0).payload());

        client.acknowledge(stream, group, messages.get(0).id());
        assertTrue(client.read(stream, group, "consumer-1", 10).isEmpty(),
                "确认过的消息不该再被读出来");
    }

    @Test
    void consumerGroupIsCreatedFromStreamStart() {
        String stream = "test.stream.group";
        String group = "test-group-create";
        client.ensureGroup(stream, group);
        // 重复创建不报错
        client.ensureGroup(stream, group);

        // 先投递再消费也能拿到（组不是从最新开始）
        client.publish(stream, group, Map.of("k", "v"));
        List<AgentStreamClient.StreamMessage> messages = client.read(stream, group, "consumer-1", 10);
        assertEquals(1, messages.size());
    }

    @Test
    void twoConsumersNeverGetTheSameMessage() {
        String stream = "test.stream.race";
        String group = "test-group-race";
        client.ensureGroup(stream, group);

        client.publish(stream, group, Map.of("n", 1));
        client.publish(stream, group, Map.of("n", 2));

        List<AgentStreamClient.StreamMessage> first = client.read(stream, group, "consumer-a", 10);
        List<AgentStreamClient.StreamMessage> second = client.read(stream, group, "consumer-b", 10);

        assertEquals(2, first.size() + second.size(), "两条消息被重复认领或丢失");
        long firstIds = first.stream().map(AgentStreamClient.StreamMessage::id).distinct().count();
        long secondIds = second.stream().map(AgentStreamClient.StreamMessage::id).distinct().count();
        assertEquals(first.size(), firstIds, "同一次认领里出现重复消息");
        assertEquals(second.size(), secondIds, "同一次认领里出现重复消息");
    }

    @Test
    void deadLetterAndRepublishKeepOriginalPayload() {
        String stream = "test.stream.dlq";
        String dlq = "agent.run.dlq";
        String group = "test-group-dlq";
        client.ensureGroup(stream, group);

        client.publish(stream, group, Map.of("a", 1));
        List<AgentStreamClient.StreamMessage> messages = client.read(stream, group, "consumer-1", 10);
        assertEquals(1, messages.size());

        AgentStreamClient.StreamMessage message = messages.get(0);
        client.publishDeadLetter(dlq, message, "boom");
        client.republish(stream, group, message, 2);

        List<AgentStreamClient.StreamMessage> afterRepublish =
                client.read(stream, group, "consumer-1", 10);
        assertEquals(1, afterRepublish.size());
        assertEquals(2, afterRepublish.get(0).retryCount());
        assertEquals("{\"a\":1}", afterRepublish.get(0).payload());

        // 死信流里能读到那条消息，并带上了原因
        List<AgentStreamClient.StreamMessage> dead = client.read(dlq, "java-dlq-reader", "reader", 10);
        assertEquals(1, dead.size());
        assertEquals("boom", dead.get(0).fields().get(AgentStreamClient.FIELD_DLQ_REASON));
    }

    @Test
    void pendingMessagesCanBeClaimedBack() {
        String stream = "test.stream.pending";
        String group = "test-group-pending";
        client.ensureGroup(stream, group);

        client.publish(stream, group, Map.of("x", 1));
        List<AgentStreamClient.StreamMessage> messages = client.read(stream, group, "consumer-crashed", 10);
        assertEquals(1, messages.size());
        // 不 ack，模拟消费者崩了

        List<AgentStreamClient.StreamMessage> claimed =
                client.claimPending(stream, group, "consumer-new", 0L, 10);
        assertEquals(1, claimed.size());
        assertEquals("{\"x\":1}", claimed.get(0).payload());
    }

    @Test
    void javaDtoFieldNamesMatchPythonContract() throws Exception {
        // Python 侧 src/app/worker/agent_run_worker.py 的 AgentRunJobMessage alias 必须完全一致，
        // 字段名一变 Python 会校验失败、消息被静默丢进死信流。
        String json = objectMapper.writeValueAsString(AgentRunJobMessage.builder()
                .jobType("START")
                .runId(1L)
                .sessionId(2L)
                .resumeId(3L)
                .sceneCode("JD_CUSTOMIZE")
                .targetSectionId(4L)
                .userInput("帮我优化")
                .jobDescription("Java 后端")
                .build());
        assertTrue(json.contains("\"jobType\":\"START\""), json);
        assertTrue(json.contains("\"runId\":1"), json);
        assertTrue(json.contains("\"sessionId\":2"), json);
        assertTrue(json.contains("\"resumeId\":3"), json);
        assertTrue(json.contains("\"sceneCode\":\"JD_CUSTOMIZE\""), json);
        assertTrue(json.contains("\"targetSectionId\":4"), json);
        assertTrue(json.contains("\"userInput\":\"帮我优化\""), json);
        assertTrue(json.contains("\"jobDescription\":\"Java 后端\""), json);
        assertFalse(json.contains("job_type"), "不能出现 Python 的 snake_case 名：" + json);
    }

    @Test
    void streamFieldNamesMatchPythonContract() {
        // Python 侧 src/app/stream/agent_stream.py 的字段名
        assertEquals("payload", AgentStreamClient.FIELD_PAYLOAD);
        assertEquals("retryCount", AgentStreamClient.FIELD_RETRY_COUNT);
        assertEquals("dlqReason", AgentStreamClient.FIELD_DLQ_REASON);
    }
}
