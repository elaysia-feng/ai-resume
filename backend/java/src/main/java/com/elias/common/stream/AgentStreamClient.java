package com.elias.common.stream;

import java.util.List;
import java.util.Map;

/**
 * Agent 与面试任务队列的消息边界。
 *
 * <p>调用方通过该接口投递、认领和确认 SQLite 消息，不直接依赖队列存储实现。</p>
 */
public interface AgentStreamClient {

    String FIELD_PAYLOAD = "payload";
    String FIELD_RETRY_COUNT = "retryCount";
    String FIELD_DLQ_REASON = "dlqReason";

    void ensureGroup(String stream, String group);

    Long publish(String stream, Object payload);

    Long publish(String stream, String group, Object payload);

    List<StreamMessage> read(String stream, String group, String consumer, int count);

    void acknowledge(String stream, String group, long taskId);

    void publishDeadLetter(String deadLetterStream, StreamMessage message, String reason);

    void republish(String stream, String group, StreamMessage message, int retryCount);

    List<StreamMessage> claimPending(String stream, String group, String consumer, long minIdleMs, int count);

    /** 队列中可被认领、确认或重新投递的一条消息。 */
    record StreamMessage(long id, Map<String, String> fields) {

        /**
         * 返回任务消息 JSON。
         *
         * @return 消息体 JSON；字段缺失时返回 null
         */
        public String payload() {
            return fields.get(FIELD_PAYLOAD);
        }

        /**
         * 返回重试次数。
         *
         * @return 已重试次数；字段缺失或格式错误时返回 0
         */
        public int retryCount() {
            try {
                return Integer.parseInt(fields.getOrDefault(FIELD_RETRY_COUNT, "0"));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }
}
