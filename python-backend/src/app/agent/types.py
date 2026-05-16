from typing import Any, Literal

from pydantic import BaseModel, Field


class AgentRouteDecision(BaseModel):
    """Supervisor Agent 的路由决策。"""

    next_node: str = Field(..., alias="nextNode", description="下一步要进入的 LangGraph 节点名称")
    reason: str = Field(..., description="选择该路由的原因，方便调试和前端解释")
    clarification_needed: bool = Field(default=False, alias="clarificationNeeded", description="是否需要先向用户追问补充信息")


class JdAnalysis(BaseModel):
    """岗位 JD 结构化分析结果。"""

    target_position: str | None = Field(default=None, alias="targetPosition", description="JD 中识别出的目标岗位名称")
    must_have_keywords: list[str] = Field(default_factory=list, alias="mustHaveKeywords", description="JD 中明确要求必须具备的关键词")
    nice_to_have_keywords: list[str] = Field(default_factory=list, alias="niceToHaveKeywords", description="JD 中加分项或优先项关键词")
    responsibility_keywords: list[str] = Field(default_factory=list, alias="responsibilityKeywords", description="岗位职责相关关键词")
    deliverable_keywords: list[str] = Field(default_factory=list, alias="deliverableKeywords", description="交付物、成果或业务目标相关关键词")
    priority_sections: list[str] = Field(default_factory=list, alias="prioritySections", description="建议优先优化的简历模块编码或标识")


class GapReport(BaseModel):
    """简历与 JD 的差距分析。"""

    matched_keywords: list[str] = Field(default_factory=list, alias="matchedKeywords", description="当前简历已经覆盖的 JD 关键词")
    missing_keywords: list[str] = Field(default_factory=list, alias="missingKeywords", description="当前简历缺失或表达不足的 JD 关键词")
    priority_sections: list[str] = Field(default_factory=list, alias="prioritySections", description="差距较明显、建议优先处理的简历模块")
    gap_summary: str = Field(default="", alias="gapSummary", description="简历与 JD 匹配差距的简短总结")
    details: list[str] = Field(default_factory=list, description="更细的差距说明列表")


class ReferenceChunk(BaseModel):
    """检索到的参考片段。"""

    text: str = Field(..., description="检索命中的参考表达或知识片段正文")
    source: str | None = Field(default=None, description="参考片段来源，例如文档名、数据集标识或 URL")
    score: float | None = Field(default=None, description="向量检索相似度分数")
    metadata: dict[str, Any] = Field(default_factory=dict, description="参考片段附加元数据，例如模块类型、标签")


class RetrievalQuery(BaseModel):
    """Retriever Agent 生成的单条检索请求。"""

    # query 是真正送去 embedding 的文本，应尽量包含岗位方向、目标模块和缺失关键词。
    query: str = Field(..., description="用于向量检索的自然语言 query")
    # module/kind/occupation 会转成 Qdrant payload filter，先缩小候选范围再做向量搜索。
    module: str | None = Field(default=None, description="可选 payload 过滤字段，例如 GENERAL / PROJECTS")
    kind: str | None = Field(default=None, description="可选 payload 过滤字段，例如 occupation_profile / section_patterns")
    occupation: str | None = Field(default=None, description="可选职业画像过滤字段，例如 编程与 AI 应用")
    # tags 暂不直接作为 Qdrant filter，主要用于调试和后续扩展。
    tags: list[str] = Field(default_factory=list, description="辅助说明标签，当前主要用于构造 query 和调试")
    reason: str = Field(default="", description="为什么需要这条检索")


class RetrievalPlan(BaseModel):
    """Retriever Agent 的检索计划。"""

    # 信息足够简单或无参考价值时可以不检索，避免无意义查库。
    should_retrieve: bool = Field(default=True, alias="shouldRetrieve", description="是否需要检索参考知识")
    # 如果首次过滤召回不足，retriever_node 会放宽过滤条件兜底检索。
    min_results: int = Field(default=2, alias="minResults", description="期望至少召回的参考片段数量")
    # 多条 query 允许 Agent 从规则、模块写法、职业画像等不同角度召回。
    queries: list[RetrievalQuery] = Field(default_factory=list, description="按优先级排列的检索请求")
    reason: str = Field(default="", description="整体检索策略说明")


class ResumeSectionPatch(BaseModel):
    """简历模块修改提案。"""

    patch_id: str = Field(..., alias="patchId", description="本条修改提案的唯一标识，用于用户确认或拒绝")
    section_id: int = Field(..., alias="sectionId", description="要修改的简历模块 ID，必须来自当前简历快照")
    section_code: str = Field(..., alias="sectionCode", description="要修改的简历模块编码，例如 PROJECT_EXPERIENCE")
    section_title: str = Field(..., alias="sectionTitle", description="要修改的简历模块标题，用于前端展示")
    operation: Literal["REPLACE_SECTION_CONTENT"] = Field(..., description="修改操作类型，P1 固定为替换整个模块内容")
    reason: str = Field(..., description="生成这条修改提案的原因")
    before_json: dict[str, Any] = Field(default_factory=dict, alias="beforeJson", description="修改前的模块内容 JSON，用于冲突检测和 diff 展示")
    after_json: dict[str, Any] = Field(default_factory=dict, alias="afterJson", description="修改后的模块内容 JSON，用户确认后由 Java 应用")
    change_summary: str = Field(..., alias="changeSummary", description="本条修改的简短摘要")
    risk_level: Literal["LOW", "MEDIUM", "HIGH"] = Field(default="LOW", alias="riskLevel", description="修改风险等级，用于提示是否可能涉及事实变化")


class ReviewResult(BaseModel):
    """Reviewer Agent 审查结果。"""

    passed: bool = Field(..., description="审查是否通过，通过后才能进入用户确认阶段")
    notes: list[str] = Field(default_factory=list, description="审查通过时的备注或风险提示")
    rejected_reasons: list[str] = Field(default_factory=list, alias="rejectedReasons", description="审查不通过的原因列表")


class ClarificationQuestion(BaseModel):
    """Agent 追问问题。"""

    field_key: str = Field(..., alias="fieldKey", description="追问字段标识，用户回答后用于写回对应上下文")
    question: str = Field(..., description="需要展示给用户的追问内容")


class ClarificationPayload(BaseModel):
    """等待用户补充的信息。"""

    run_id: int = Field(..., alias="runId", description="需要补充信息的 Agent run ID")
    questions: list[ClarificationQuestion] = Field(default_factory=list, description="本次需要用户回答的问题列表")


class ApprovalPayload(BaseModel):
    """等待用户确认的 patch 包。"""

    run_id: int = Field(..., alias="runId", description="等待用户确认的 Agent run ID")
    resume_id: int = Field(..., alias="resumeId", description="本次修改对应的简历 ID")
    summary: str = Field(..., description="本次 AI 修改建议的整体摘要")
    risk_notes: list[str] = Field(default_factory=list, alias="riskNotes", description="整体风险提示或审查备注")
    patches: list[ResumeSectionPatch] = Field(default_factory=list, description="等待用户确认的简历修改提案列表")
