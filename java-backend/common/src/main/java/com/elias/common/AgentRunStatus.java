package com.elias.common;

/**
 * Agent run 状态码
 * <p>
 * 与 Python 端 {@code AgentStatus} 保持一致，Java / Python 两侧回写状态时
 * 必须使用本枚举的 {@link #code} 值，禁止硬编码字符串。
 * </p>
 *
 * 状态流转：
 * <pre>
 *   PENDING → QUEUED → RUNNING ──→ SUCCESS
 *                          │          ↑
 *                          ├─→ WAITING_USER ──→ QUEUED (continue)
 *                          ├─→ WAITING_CONFIRM ──→ SUCCESS (approve)
 *                          ├─→ FAILED
 *                          └─→ CANCELLED
 * </pre>
 */
public enum AgentRunStatus {

    /** 已创建 run 记录，但还没有投入 MQ，不会被 Python worker 消费。 */
    PENDING("PENDING"),

    /** 已投入 RabbitMQ，等待 Python worker 消费执行。 */
    QUEUED("QUEUED"),

    /** 图正在执行中，节点会持续产生阶段事件。 */
    RUNNING("RUNNING"),

    /** clarifier 节点挂起，等待用户补充信息后 continue 恢复。 */
    WAITING_USER("WAITING_USER"),

    /** approval_packager 节点挂起，等待用户确认是否应用 patch。 */
    WAITING_CONFIRM("WAITING_CONFIRM"),

    /** run 正常完成，结果已写入 resultSummary。 */
    SUCCESS("SUCCESS"),

    /** run 执行失败，错误信息写入 errorMessage。 */
    FAILED("FAILED"),

    /** 用户或 Java 网关主动取消了该 run。 */
    CANCELLED("CANCELLED");

    private final String code;

    AgentRunStatus(String code) {
        this.code = code;
    }

    /** 返回写入数据库 / 传输给 Python 的状态码字符串。 */
    public String getCode() {
        return code;
    }
}
