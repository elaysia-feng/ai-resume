package com.elias.common.stream.impl;
import com.elias.common.stream.AgentStreamClient;
import com.elias.common.stream.AgentStreamClient.StreamMessage;
import com.elias.common.stream.AgentStreamProperties;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agent 任务队列的 SQLite 实现（顶替原来的 Redis Stream）。
 *
 * <p>一条任务 = agent_task 表里的一行，按 (stream, group_name) 划分消费组。</p>
 *
 * <p>认领用的是一条 {@code UPDATE ... WHERE id IN (SELECT ...)}：
 * SQLite 单条写语句本身是原子的，所以两个消费者同时认领时只有一个能拿到，
 * 另一个会被 busy_timeout 挡一会儿后拿到 0 行，不会重复消费。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentStreamClientImpl implements AgentStreamClient {

    /** 消息体字段名：整条消息的 JSON。 */
    public static final String FIELD_PAYLOAD = "payload";

    /** 消息体字段名：已重试次数。 */
    public static final String FIELD_RETRY_COUNT = "retryCount";

    /** 死信流专用字段名：进入死信的原因。 */
    public static final String FIELD_DLQ_REASON = "dlqReason";

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_CLAIMED = "CLAIMED";
    private static final String STATUS_DONE = "DONE";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final AgentStreamProperties agentStreamProperties;

    /**
     * 登记消费组（幂等）。
     */
    public void ensureGroup(String stream, String group) {
        jdbcTemplate.update(
                "INSERT OR IGNORE INTO agent_stream_group (stream, group_name, created_at) VALUES (?, ?, ?)",
                stream, group, Timestamp.valueOf(LocalDateTime.now()));
    }

    /**
     * 投递一条 JSON 消息，返回任务 ID。
     *
     * <p>消费组由流名推导：生产者必须和消费者约定好这一组属于谁。</p>
     */
    public Long publish(String stream, Object payload) {
        return publish(stream, agentStreamProperties.groupOf(stream), payload);
    }

    /**
     * 投递一条 JSON 消息到指定消费组，返回任务 ID。
     */
    public Long publish(String stream, String group, Object payload) {
        String body;
        try {
            body = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("序列化任务消息失败: " + stream, e);
        }
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                "INSERT INTO agent_task (stream, group_name, payload, status, created_at, updated_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?)",
                stream, group, body, STATUS_PENDING, Timestamp.valueOf(now), Timestamp.valueOf(now));
        Long id = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
        return id;
    }

    /**
     * 认领并读取一批新消息。
     */
    public List<StreamMessage> read(String stream, String group, String consumer, int count) {
        String token = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                "UPDATE agent_task SET status = ?, consumer = ?, claim_token = ?, claimed_at = ?, updated_at = ?"
                        + " WHERE id IN (SELECT id FROM agent_task"
                        + "   WHERE stream = ? AND group_name = ? AND status = ?"
                        + "   ORDER BY id LIMIT ?)",
                STATUS_CLAIMED, consumer, token, Timestamp.valueOf(now), Timestamp.valueOf(now),
                stream, group, STATUS_PENDING, count);

        List<StreamMessage> messages = jdbcTemplate.query(
                "SELECT id, payload, retry_count, dlq_reason FROM agent_task WHERE claim_token = ? ORDER BY id",
                (rs, rowNum) -> {
                    Map<String, String> fields = new HashMap<>();
                    fields.put(FIELD_PAYLOAD, rs.getString("payload"));
                    fields.put(FIELD_RETRY_COUNT, String.valueOf(rs.getInt("retry_count")));
                    String dlqReason = rs.getString("dlq_reason");
                    if (dlqReason != null) {
                        fields.put(FIELD_DLQ_REASON, dlqReason);
                    }
                    return new StreamMessage(rs.getLong("id"), fields);
                }, token);
        return messages;
    }

    /**
     * 确认消息已处理完成。
     */
    public void acknowledge(String stream, String group, long taskId) {
        jdbcTemplate.update(
                "UPDATE agent_task SET status = ?, updated_at = ? WHERE id = ?",
                STATUS_DONE, Timestamp.valueOf(LocalDateTime.now()), taskId);
    }

    /**
     * 把消息转投到死信流。
     */
    public void publishDeadLetter(String deadLetterStream, StreamMessage message, String reason) {
        String group = agentStreamProperties.groupOf(deadLetterStream);
        String body = message.payload() == null ? "" : message.payload();
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                "INSERT INTO agent_task (stream, group_name, payload, status, dlq_reason, created_at, updated_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?)",
                deadLetterStream, group, body, STATUS_PENDING,
                reason == null ? "" : reason,
                Timestamp.valueOf(now), Timestamp.valueOf(now));
        jdbcTemplate.update(
                "UPDATE agent_task SET status = ?, updated_at = ? WHERE id = ?",
                STATUS_DONE, Timestamp.valueOf(now), message.id());
    }

    /**
     * 按原样重新投递一条消息（用于消费失败后的重试）。
     */
    public void republish(String stream, String group, StreamMessage message, int retryCount) {
        jdbcTemplate.update(
                "UPDATE agent_task SET status = ?, retry_count = ?, claim_token = NULL, consumer = NULL,"
                        + " claimed_at = NULL, updated_at = ? WHERE id = ?",
                STATUS_PENDING, retryCount, Timestamp.valueOf(LocalDateTime.now()), message.id());
    }

    /**
     * 回收消费者崩溃后遗留的未确认消息。
     *
     * <p>先把这些行改回 PENDING，再正常读一次。</p>
     */
    public List<StreamMessage> claimPending(String stream, String group, String consumer,
                                           long minIdleMs, int count) {
        jdbcTemplate.update(
                "UPDATE agent_task SET status = ?, claim_token = NULL, consumer = NULL, claimed_at = NULL,"
                        + " updated_at = ? WHERE stream = ? AND group_name = ? AND status = ? AND claimed_at < ?",
                STATUS_PENDING, Timestamp.valueOf(LocalDateTime.now()),
                stream, group, STATUS_CLAIMED,
                Timestamp.valueOf(LocalDateTime.now().minus(Duration.ofMillis(minIdleMs))));
        return read(stream, group, consumer, count);
    }

}
