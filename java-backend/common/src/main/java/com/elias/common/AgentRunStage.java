package com.elias.common;

/**
 * Agent run 阶段码
 * <p>
 * 与 Python 端 {@code AgentStage} / {@code InterviewStage} 保持一致，
 * 标识 run 当前执行到图中的哪个节点。Python 回写 currentStage 时
 * 必须使用本枚举的 {@link #code} 值，禁止硬编码字符串。
 * </p>
 *
 * <h3>简历优化流程阶段</h3>
 * <pre>
 *   BOOTSTRAP → SUPERVISOR ──→ JD_ANALYST → GAP_ANALYZER → RETRIEVER
 *                  │                                              ↓
 *                  ├─→ CLARIFIER (WAITING_USER)               REWRITER
 *                  │                                              ↓
 *                  │                                         REVIEWER
 *                  │                                              ↓
 *                  └─────────────────────────────────── APPROVAL_PACKAGER
 * </pre>
 *
 * <h3>面试模拟流程阶段</h3>
 * <pre>
 *   BOOTSTRAP → ANALYST → QUESTION (WAITING_ANSWER) → ANSWER → ANALYST_ANSWER → SUMMARY
 * </pre>
 */
public enum AgentRunStage {

    // ==================== 通用阶段 ====================

    /** 启动阶段：加载简历、会话、schema 等上下文。 */
    BOOTSTRAP("BOOTSTRAP"),

    /** 记忆压缩阶段：把较早对话整理成长期摘要。 */
    MEMORY_SUMMARY("MEMORY_SUMMARY"),

    /** 调度阶段：判断继续主链路，还是先追问用户。 */
    SUPERVISOR("SUPERVISOR"),

    /** JD 分析阶段：提取岗位职责、技能和匹配重点。 */
    JD_ANALYST("JD_ANALYST"),

    /** 差距分析阶段：对比 JD 和当前简历，找出需要强化的部分。 */
    GAP_ANALYZER("GAP_ANALYZER"),

    /** 检索阶段：召回参考表达、知识片段或历史素材。 */
    RETRIEVER("RETRIEVER"),

    /** 改写阶段：生成简历 section 的修改提案。 */
    REWRITER("REWRITER"),

    /** 审查阶段：检查改写内容是否符合约束，决定通过或重写。 */
    REVIEWER("REVIEWER"),

    /** 追问阶段：信息不足时生成问题，挂起等待用户回答。 */
    CLARIFIER("CLARIFIER"),

    /** 审批打包阶段：把可确认的 patch 封装给前端展示。 */
    APPROVAL_PACKAGER("APPROVAL_PACKAGER"),

    // ==================== 面试模拟阶段 ====================

    /** 面试启动：加载面试上下文、简历、岗位信息。 */
    ANALYST("ANALYST"),

    /** 出题阶段：根据分析结果生成面试题目。 */
    QUESTION("QUESTION"),

    /** 等待回答：题目已发出，等待用户选择或输入答案。 */
    WAITING_ANSWER("WAITING_ANSWER"),

    /** 回答阶段：用户已提交答案，正在处理。 */
    ANSWER("ANSWER"),

    /** 回答分析：分析用户回答，判断是否需要追问或进入下一题。 */
    ANALYST_ANSWER("ANALYST_ANSWER"),

    /** 总结阶段：面试结束，生成面试表现总结。 */
    SUMMARY("SUMMARY");

    private final String code;

    AgentRunStage(String code) {
        this.code = code;
    }

    /** 返回写入数据库 / 传输给 Python 的阶段码字符串。 */
    public String getCode() {
        return code;
    }
}
