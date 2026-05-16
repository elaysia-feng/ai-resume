from typing import Any, TypedDict

from langgraph.graph import MessagesState


class ResumeAgentInput(MessagesState, total=False):
    """Studio 和 FastAPI 入口需要填写的最小输入。

    这个类型只控制 LangGraph Studio 里 Input 面板展示哪些字段。
    如果直接用 ResumeAgentState，Studio 会把所有中间状态字段都展开，画布很难看清。
    继承 MessagesState 后，Studio 的 Chat 面板和 messages 自动追加能力都能使用。
    """

    # 三个 ID 来自 Java，Python 不自己创建业务主键。
    run_id: int
    session_id: int
    resume_id: int

    # scene_code 决定本轮走哪个业务场景，例如 JD_CUSTOMIZE。
    scene_code: str
    # 本轮用户补充的自然语言要求，例如“帮我按这个岗位优化项目经历”。
    user_input: str | None
    # 目标岗位 JD；如果入口没传，bootstrap_node 会尝试从 Java session 里补。
    job_description: str | None

    # Java 明确指定本轮允许 AI 修改的简历模块 ID，避免模型越权改整份简历。
    target_section_id: int

    # Studio 本地调试时可以直接传 mock 数据，避免必须启动 Java。
    resume_snapshot: dict[str, Any]
    # sectionCode -> JSON Schema，reviewer 用它校验 patch 的 afterJson。
    section_schemas: dict[str, dict[str, Any]]
    # Java 持久化的历史消息，bootstrap_node 会转换成 LangChain messages。
    history_messages: list[dict[str, Any]]
    # 会话长期摘要；记忆压缩后会继续写回 Java，后续 run 复用。
    summary: str


class ResumeAgentOutput(MessagesState, total=False):
    """Studio 输出面板展示的关键结果。

    这里只放最终调试最关心的字段，避免输出面板被完整 state 淹没。
    messages 会由 LangGraph 的 add_messages reducer 自动维护。
    """

    run_id: int
    status: str
    current_stage: str
    summary: str
    clarification_payload: dict[str, Any]
    approval_payload: dict[str, Any]
    errors: list[str]


class ResumeAgentState(MessagesState, total=False):
    """LangGraph 多 Agent 共享状态。

    每个 node 都接收这个 state，并返回局部更新后的 state。
    注意：这里是 graph 内部流转用的完整状态，不等于 Studio 输入表单。
    继承 MessagesState 后，state["messages"] 使用 LangGraph 内置 add_messages reducer：
    节点只要返回 {"messages": [新消息]}，新消息就会追加到历史消息里，而不是覆盖。

    运行顺序：
    1. bootstrap_node 写入简历快照、schema、历史消息和基础约束。
    2. supervisor_node 判断信息是否足够，决定进入追问还是主链路。
    3. jd_analyst_node / gap_analyzer_node 提取岗位要求和简历差距。
    4. retriever_node 生成检索计划并写入 RAG 召回片段。
    5. rewriter_node 基于事实、差距和参考片段生成候选 patch。
    6. reviewer_node 做 schema、越权和事实风险审查。
    7. approval_packager_node 把通过审查的 patch 打包给前端确认。

    字段使用规则：
    1. ID、scene、target_section_id 是 Java 传入的边界，Python 不主动改。
    2. resume_snapshot 是事实来源，模型不能写入不存在的经历。
    3. *_analysis、*_report、retrieval_plan、retrieved_chunks 是中间产物。
    4. candidate_patches 只是建议，只有用户确认后才由 Java 应用。
    5. route_decision、review_passed 只给 LangGraph conditional_edges 使用。
    """

    # 1. 本轮 run 的基础身份信息。Python 不生成业务主键，只沿用 Java 传入的 ID。
    run_id: int
    session_id: int
    resume_id: int
    # 场景码决定 supervisor 的判断口径，例如 JD_CUSTOMIZE 缺 JD 时要追问。
    scene_code: str
    # 当前 run 状态，主要用于 SSE 和 Java 状态同步。
    status: str
    # 当前业务阶段码，前端用它展示进度。
    current_stage: str

    # 2. 用户输入和补充信息。supervisor / jd_analyst / retriever 都会读取这些字段。
    # 本轮用户原始要求，可能只是“帮我优化”，也可能直接包含 JD。
    user_input: str | None
    # 目标岗位 JD；jd_analyst 优先读取这个字段。
    job_description: str | None
    # 本轮唯一允许修改的 section id；rewriter/reviewer 都会硬限制它。
    target_section_id: int
    # clarifier interrupt 恢复后写入的用户回答列表。
    clarification_answers: list[dict[str, Any]]

    # 3. bootstrap_node 从 Java 加载出来的权威上下文。
    # 3.1 resume_snapshot 是后续所有“不能编造事实”校验的基础。
    # 3.2 section_schemas 是 patch 写库前的结构边界。
    # 3.3 constraints / editable_section_ids 是业务权限边界。
    # 当前简历完整快照；rewriter 只从这里读取事实，不能凭空补经历。
    resume_snapshot: dict[str, Any]
    # 当前简历各模块 schema；reviewer 写库前做确定性结构校验。
    section_schemas: dict[str, dict[str, Any]]
    # Java 保存的历史对话；只在 bootstrap 阶段转成 messages。
    history_messages: list[dict[str, Any]]
    # Java 允许编辑的模块集合；后续可用于更细的越权校验。
    editable_section_ids: list[int]
    # Java 返回的业务约束，例如可编辑范围、用户限制或风控规则。
    constraints: dict[str, Any]

    # 4. 各业务节点逐步写入的中间结果。
    # 4.1 summary 是长期记忆；messages 只保留最近原始对话。
    # 4.2 jd_analysis 和 gap_report 决定“要改什么”。
    # 4.3 retrieval_plan 和 retrieved_chunks 决定“参考什么”。
    # 4.4 candidate_patches 和 review_result 决定“能否给用户确认”。
    # 长期记忆摘要；summarize_conversation_node 或 compact_memory_for_model 写入。
    summary: str
    # jd_analyst 输出的结构化 JD 分析，供 gap_analyzer/retriever/rewriter 使用。
    jd_analysis: dict[str, Any]
    # gap_analyzer 输出的差距报告，retriever 会优先用 missingKeywords 构造 query。
    gap_report: dict[str, Any]
    # retriever 输出的检索计划，记录查了什么、为什么查，便于调试 Agentic RAG。
    retrieval_plan: dict[str, Any]
    # retriever 写入的 Qdrant 召回片段，rewriter 会把它作为参考表达。
    retrieved_chunks: list[dict[str, Any]]
    # rewriter 生成的候选 patch；这里只是建议，不直接修改简历。
    candidate_patches: list[dict[str, Any]]
    # reviewer 的结构化审查结果。
    review_result: dict[str, Any]

    # 5. 两种需要前端介入的挂起结果。
    # 5.1 clarification_payload：信息不足，前端展示追问。
    # 5.2 approval_payload：生成修改建议，前端展示审批。
    # approval_packager 写入，前端展示给用户确认是否应用 patch。
    approval_payload: dict[str, Any]
    # clarifier 写入，前端展示追问表单；用户回答后通过 continue 恢复图。
    clarification_payload: dict[str, Any]

    # 6. 路由和审查状态，只给 conditional_edges 判断下一跳使用。
    # 6.1 route_decision 控制 supervisor -> jd_analyst / clarifier。
    # 6.2 review_passed 控制 reviewer -> approval_packager / rewriter / failed。
    # supervisor 写入，decide_supervisor_route 读取，决定进主链路还是追问。
    route_decision: dict[str, Any]
    # reviewer 写入，decide_review_route 读取，决定通过、重写或失败。
    review_passed: bool
    # 审查备注或拒绝原因，approval_packager 会合并给前端展示。
    review_notes: list[str]

    # 7. 记忆压缩控制字段：记录上次摘要时的 token 规模，避免同一批消息反复摘要。
    memory_last_summary_token_count: int

    # 8. 运行控制字段。
    # 8.1 review_retry_count 防止 reviewer 和 rewriter 无限循环。
    # 8.2 event_seq 保证 SSE 事件顺序。
    # 8.3 errors 收集运行异常，最终给 Java/前端展示。
    # reviewer 不通过时递增，超过阈值后不再无限重写。
    review_retry_count: int
    # SSE 事件序号，AgentGraphService 每发一条事件递增。
    event_seq: int
    # 节点失败或网关异常时追加错误信息，最终同步给 Java/前端。
    errors: list[str]
