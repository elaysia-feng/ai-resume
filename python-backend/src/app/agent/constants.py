"""Agent 运行常量。"""


class AgentStatus:
    """Agent run 状态。"""

    # 已创建 run，但还没有真正进入图执行。
    PENDING = "PENDING"
    # 已进入 RabbitMQ，等待 Python worker 消费。
    QUEUED = "QUEUED"
    # 图正在执行，节点会持续产生阶段事件。
    RUNNING = "RUNNING"
    # clarifier 节点挂起，等待用户补充信息后 continue。
    WAITING_USER = "WAITING_USER"
    # approval_packager 节点挂起，等待用户确认是否应用 patch。
    WAITING_CONFIRM = "WAITING_CONFIRM"
    # run 正常完成。
    SUCCESS = "SUCCESS"
    # run 执行失败，错误信息通常写入 errorMessage / errors。
    FAILED = "FAILED"
    # 用户或 Java 网关主动取消 run。
    CANCELLED = "CANCELLED"


class AgentStage:
    """Agent 阶段码。"""

    # 启动阶段：加载简历、会话、schema 等上下文。
    BOOTSTRAP = "BOOTSTRAP"
    # 记忆压缩阶段：把较早对话整理成长期摘要。
    MEMORY_SUMMARY = "MEMORY_SUMMARY"
    # 调度阶段：判断是继续主链路，还是先追问用户。
    SUPERVISOR = "SUPERVISOR"
    # JD 分析阶段：提取岗位职责、技能和匹配重点。
    JD_ANALYST = "JD_ANALYST"
    # 差距分析阶段：对比 JD 和当前简历，找出需要强化的部分。
    GAP_ANALYZER = "GAP_ANALYZER"
    # 检索阶段：召回参考表达、知识片段或历史素材。
    RETRIEVER = "RETRIEVER"
    # 改写阶段：生成简历 section 的修改提案。
    REWRITER = "REWRITER"
    # 审查阶段：检查改写内容是否符合约束，决定通过或重写。
    REVIEWER = "REVIEWER"
    # 追问阶段：信息不足时生成问题，并挂起等待用户回答。
    CLARIFIER = "CLARIFIER"
    # 审批打包阶段：把可确认的 patch 封装给前端展示。
    APPROVAL_PACKAGER = "APPROVAL_PACKAGER"


class AgentEventType:
    """SSE 事件类型。"""

    # run 开始。
    RUN_STARTED = "run.started"
    # 当前执行阶段变化。
    STAGE_CHANGED = "stage.changed"
    # 工具调用开始。
    TOOL_STARTED = "tool.started"
    # 工具调用结束。
    TOOL_FINISHED = "tool.finished"
    # Assistant 流式文本增量。
    ASSISTANT_DELTA = "assistant.delta"
    # 需要用户补充信息。
    CLARIFICATION_REQUIRED = "clarification.required"
    # 需要用户确认 patch。
    APPROVAL_REQUIRED = "approval.required"
    # run 完成。
    RUN_COMPLETED = "run.completed"
    # run 失败。
    RUN_FAILED = "run.failed"


class AgentRoute:
    """LangGraph 路由结果。"""

    # supervisor 判定信息足够，进入 JD 分析链路。
    JD_ANALYST = "jd_analyst"
    # supervisor 判定信息不足，进入追问节点。
    CLARIFIER = "clarifier"
    # reviewer 判定需要重写，回到改写节点。
    REWRITER = "rewriter"
    # reviewer 判定通过，进入 patch 审批打包节点。
    APPROVAL_PACKAGER = "approval_packager"
    # 路由失败或达到失败条件，结束图执行。
    FAILED = "failed"


class PatchOperation:
    """Patch 操作类型。"""

    # 替换指定简历 section 的结构化内容。
    REPLACE_SECTION_CONTENT = "REPLACE_SECTION_CONTENT"


class InterviewStage:
    """面试模块阶段码。"""

    BOOTSTRAP = "BOOTSTRAP"
    ANALYST = "ANALYST"
    QUESTION = "QUESTION"
    WAITING_ANSWER = "WAITING_ANSWER"
    ANSWER = "ANSWER"
    ANALYST_ANSWER = "ANALYST_ANSWER"
    SUMMARY = "SUMMARY"


# 面试节点名 -> 阶段码
INTERVIEW_NODE_STAGE_MAP = {
    "bootstrap": InterviewStage.BOOTSTRAP,
    "analyst": InterviewStage.ANALYST,
    "question": InterviewStage.QUESTION,
    "answer": InterviewStage.ANSWER,
    "analyst_answer": InterviewStage.ANALYST_ANSWER,
    "summary": InterviewStage.SUMMARY,
}

# 面试节点名 -> 面向用户的阶段提示
INTERVIEW_NODE_MESSAGE_MAP = {
    "bootstrap": "加载面试上下文",
    "analyst": "分析简历和岗位方向",
    "question": "正在出题",
    "answer": "获取用户回答",
    "analyst_answer": "分析回答并判断下一题",
    "summary": "生成面试总结",
}
