import asyncio
import json
from typing import Any

from langchain_core.messages import HumanMessage, SystemMessage

from src.app.agent.agentic_rag.search_rag_with_bm25.query import rag_query
from src.app.agent.constants import AgentStage
from src.app.agent.memory import build_memory_messages, compact_memory_for_model
from src.app.agent.prompts.retriever_prompt import RETRIEVER_SYSTEM_PROMPT
from src.app.agent.state import ResumeAgentState
from src.app.agent.tools.retrieval_tools import search_reference_chunks_tool
from src.app.agent.types import ReferenceChunk, RetrievalPlan, RetrievalQuery
from src.app.config.settings import get_settings
from src.app.service.agent_factory import agent_factory


async def retriever_node(state: ResumeAgentState) -> ResumeAgentState:
    """Retriever Agent：规划并检索参考表达和知识片段。

    执行步骤：
    1. 复制 state，避免直接污染上游节点传入对象。
    2. 让 Retriever Agent 生成 RetrievalPlan。
    3. 按 RetrievalPlan 多次调用 Qdrant 检索。
    4. 对召回结果去重、截断上下文长度。
    5. 写回 retrieval_plan 和 retrieved_chunks，交给 rewriter 使用。
    """
    input_state = dict(state)
    state = dict(state)
    # 检索结果写入 retrieved_chunks，messages 不需要重复返回。
    state.pop("messages", None)
    state["current_stage"] = AgentStage.RETRIEVER

    mode = resolve_retrieval_mode(state)
    if mode == "off":
        # A/B 评测：关闭 RAG，不调用任何检索服务。
        # rewriter 只能依据简历事实 + JD 分析 + 差距报告改写，不看任何外部参考片段。
        state["retrieved_chunks"] = []
        state["retrieval_plan"] = {
            "shouldRetrieve": False,
            "minResults": 0,
            "queries": [],
            "reason": "评测模式：关闭 RAG 检索",
        }
        state.pop("retrieval_error", None)
        return state

    plan = await build_retrieval_plan(input_state)
    if mode == "on":
        # A/B 评测：强制开启检索，即使模型认为不需要，也使用确定性兜底计划。
        plan = force_retrieval_on(plan, input_state)
    chunks = await execute_retrieval_plan(plan, input_state)
    # retrieval_plan 是给调试和前端排查用的；真正喂给 Rewriter 的是 retrieved_chunks。
    state["retrieved_chunks"] = normalize_retrieved_chunks(chunks)
    state["retrieval_plan"] = plan.model_dump(by_alias=True)
    retrieval_error = extract_retrieval_error(chunks)
    if retrieval_error:
        state["retrieval_error"] = retrieval_error
        state["errors"] = [*state.get("errors", []), retrieval_error]
    return state


async def build_retrieval_plan(state: ResumeAgentState) -> RetrievalPlan:
    """让模型生成检索计划；模型不可用时使用本地兜底计划。

    这里体现 Agentic RAG 的“先规划”：
    1. 模型读取 JD、差距报告、目标模块和历史记忆。
    2. 模型决定是否检索、查几条 query、带什么过滤条件。
    3. 本地代码再对模型输出做规模限制和兜底。
    """
    try:
        messages = build_retriever_messages(state)
        memory_updates, compacted_state = await compact_memory_for_model(dict(state), messages)
        compacted_state.update(memory_updates)
        return normalize_retrieval_plan(
            await agent_factory.invoke_agent(build_retriever_messages(compacted_state), RetrievalPlan)
        )
    except Exception:
        return build_fallback_retrieval_plan(state)


def resolve_retrieval_mode(state: ResumeAgentState) -> str:
    """解析本轮检索模式：auto(LLM 决定)/on(强制)/off(关闭)。

    优先级：单次 run 的 state["retrieval_mode"] > 全局 AGENT_RETRIEVAL_MODE > 默认 auto。
    """
    mode = (state.get("retrieval_mode") or get_settings().agent_retrieval_mode or "auto").strip().lower()
    return mode if mode in {"auto", "on", "off"} else "auto"


def force_retrieval_on(plan: RetrievalPlan, state: ResumeAgentState) -> RetrievalPlan:
    """把检索计划强制置为开启；queries 为空时使用确定性兜底计划。

    评测"RAG 开启"对照时用，保证每一轮确实发生了检索，而不是被模型跳过。
    """
    if plan.should_retrieve and plan.queries:
        return plan
    fallback = build_fallback_retrieval_plan(state)
    return RetrievalPlan(
        shouldRetrieve=True,
        minResults=fallback.minResults or plan.minResults or 2,
        queries=fallback.queries or plan.queries,
        reason=f"{plan.reason or ''} | 评测模式：强制开启 RAG 检索",
    )


def build_retriever_messages(state: ResumeAgentState) -> list:
    """构造 Retriever Agent 的模型消息。

    prompt 输入控制在检索决策需要的范围内：
    1. 目标模块：让模型知道当前优化 SUMMARY / PROJECTS / EXPERIENCE 等。
    2. 用户输入和 JD：让模型判断目标职业和岗位方向。
    3. JD 分析：提供结构化岗位关键词。
    4. 差距报告：优先检索缺失或表达不足的能力。
    """
    return [
        SystemMessage(content=RETRIEVER_SYSTEM_PROMPT),
        *build_memory_messages(state),
        HumanMessage(
            content=(
                f"目标模块：{json.dumps(get_target_section(state), ensure_ascii=False)}\n"
                f"用户输入：{state.get('user_input') or ''}\n"
                f"岗位 JD：{state.get('job_description') or ''}\n"
                f"JD 分析：{json.dumps(state.get('jd_analysis', {}), ensure_ascii=False)}\n"
                f"差距报告：{json.dumps(state.get('gap_report', {}), ensure_ascii=False)}"
            )
        ),
    ]


def normalize_retrieval_plan(plan: RetrievalPlan) -> RetrievalPlan:
    """限制检索计划规模，避免单轮检索过多。"""
    # Retriever 是 LLM 输出，必须做一次确定性收口，避免生成过多 query 拉高延迟和成本。
    queries = [query for query in plan.queries if query.query.strip()][:4]
    return RetrievalPlan(
        shouldRetrieve=plan.should_retrieve and bool(queries),
        minResults=max(1, min(plan.min_results or 2, 6)),
        queries=queries,
        reason=plan.reason,
    )


def build_fallback_retrieval_plan(state: ResumeAgentState) -> RetrievalPlan:
    """模型不可用时的确定性检索计划。

    兜底策略：
    1. 根据 JD 关键词粗略判断职业画像。
    2. 第一条 query 查 occupation_profile，获取岗位表达。
    3. 第二条 query 查 section_patterns，获取模块写法。
    4. 第三条 query 查 rewrite_policy，压住事实边界风险。
    """
    target_section = get_target_section(state)
    if not target_section:
        return RetrievalPlan(
            shouldRetrieve=False,
            minResults=1,
            queries=[],
            reason="未找到目标模块快照，跳过知识库检索",
        )
    section_code = target_section.get("sectionCode") or target_section.get("section_code") or "GENERAL"
    jd_analysis = state.get("jd_analysis", {})
    gap_report = state.get("gap_report", {})
    target_position = jd_analysis.get("targetPosition") or ""
    keywords = collect_keywords(gap_report, jd_analysis)
    occupation = infer_occupation(" ".join([target_position, *keywords, state.get("job_description") or ""]))

    base_query = " ".join(part for part in [target_position, section_code, *keywords[:5], "简历改写"] if part)
    queries = [
        # 第一条优先查职业画像，让改写风格贴近目标岗位。
        RetrievalQuery(
            query=base_query or state.get("job_description") or "简历改写 事实边界",
            module="GENERAL",
            kind="occupation_profile" if occupation else None,
            occupation=occupation,
            tags=keywords[:3],
            reason="按目标岗位和缺失关键词召回职业画像",
        ),
        # 第二条查模块写法，保证 Rewriter 知道当前 section 应该怎么表达。
        RetrievalQuery(
            query=f"{section_code} 简历模块 写法 {target_position}".strip(),
            module="GENERAL",
            kind="section_patterns",
            tags=[section_code],
            reason="召回目标模块写法",
        ),
        # 第三条固定查事实边界，压住“为了匹配 JD 而编造经历”的风险。
        RetrievalQuery(
            query="简历改写 事实边界 不编造 指标 经验",
            module="GENERAL",
            kind="rewrite_policy",
            reason="召回事实边界规则",
        ),
    ]
    queries = [query for query in queries if query.query.strip()]
    return RetrievalPlan(shouldRetrieve=bool(queries), minResults=2, queries=queries)


async def execute_retrieval_plan(plan: RetrievalPlan, state: ResumeAgentState) -> list[ReferenceChunk]:
    """执行检索计划，结果不足时放宽过滤条件再查一次。

    检索顺序：
    1. 按模型计划执行带过滤条件的检索。
    2. 先去重，避免多条 query 命中同一 chunk。
    3. 如果结果少于 min_results，去掉 kind/occupation/module 过滤后兜底检索。
    4. 最后按字符数截断，避免把过多 RAG 内容塞给 rewriter。
    """
    if not plan.should_retrieve:
        return []

    settings = get_settings()
    chunks: list[ReferenceChunk] = []
    for query in plan.queries:
        # 每条 query 可以带不同 payload filter，模拟 Agent 多步查资料。
        chunks.extend(await search_chunks(query, top_k=settings.optimize_retrieve_top_k))

    chunks = deduplicate_chunks(chunks)
    if len(chunks) < plan.min_results:
        # 第一次带 kind/occupation 过滤可能过窄；不足时用 query 本身做无过滤兜底。
        for query in plan.queries[:2]:
            fallback_query = RetrievalQuery(query=query.query, reason="过滤召回不足，放宽条件兜底检索")
            chunks.extend(await search_chunks(fallback_query, top_k=settings.optimize_retrieve_top_k))
        chunks = deduplicate_chunks(chunks)

    return limit_chunks_by_chars(chunks, settings.optimize_max_context_chars)


async def search_chunks(query: RetrievalQuery, top_k: int | None = None) -> list[ReferenceChunk]:
    """执行单次混合检索，检索失败时返回空列表，不阻断主链路。

    单次检索实际做了三件事：
    1. 把 RetrievalQuery 转成 RAG metadata filter。
    2. 调用 rag_query tool，同时走 ES BM25 和 Milvus 向量召回。
    3. 把 tool 返回结果转成现有 ReferenceChunk，交给后续 rewriter 使用。
    """
    if not query.query.strip():
        return []
    rag_error = None
    qdrant_error = None

    try:
        settings = get_settings()
        results = await asyncio.wait_for(
            asyncio.to_thread(
                rag_query.invoke,
                {
                    "text": query.query,
                    "top_k": top_k or settings.optimize_retrieve_top_k,
                    "filters": build_rag_filters(query),
                },
            ),
            timeout=max(settings.optimize_retrieve_timeout_seconds, 10),
        )
        chunks = [to_reference_chunk(item) for item in results]
        if chunks:
            return chunks
    except Exception as exc:
        rag_error = f"ES/Milvus 混合检索失败: {exc}"

    try:
        chunks = await asyncio.wait_for(
            search_reference_chunks_tool(
                query.query,
                top_k=top_k,
                module=query.module,
                kind=query.kind,
                occupation=query.occupation,
            ),
            timeout=get_settings().optimize_retrieve_timeout_seconds,
        )
        if chunks:
            return chunks
    except Exception as exc:
        qdrant_error = f"Qdrant 兜底检索失败: {exc}"

    if rag_error or qdrant_error:
        return [build_retrieval_failure_chunk(query, rag_error, qdrant_error)]
    return []


def build_retrieval_failure_chunk(
    query: RetrievalQuery,
    rag_error: str | None,
    qdrant_error: str | None,
) -> ReferenceChunk:
    errors = [item for item in [rag_error, qdrant_error] if item]
    return ReferenceChunk(
        text=(
            "知识库检索服务当前不可用。本轮不能把“无召回结果”当作知识库没有相关内容；"
            "后续改写必须只依据用户提供的简历事实、JD 分析和差距报告，保持保守表达，"
            "不要新增未经用户确认的项目、指标、经历或证书。"
        ),
        source="retrieval_failure",
        score=0,
        metadata={
            "retrievalStatus": "FAILED",
            "query": query.query,
            "errors": errors,
        },
    )


def extract_retrieval_error(chunks: list[ReferenceChunk]) -> str | None:
    for chunk in chunks:
        if chunk.metadata.get("retrievalStatus") == "FAILED":
            errors = chunk.metadata.get("errors") or []
            return "知识库检索失败，已降级为无外部参考的保守改写: " + " | ".join(errors)
    return None


def build_rag_filters(query: RetrievalQuery) -> dict[str, str]:
    """把 Retriever 计划里的过滤条件转换成 rag_query 使用的 metadata filter。"""
    filters = {"scene_code": infer_scene_code_from_query(query)}
    if query.kind:
        filters["kind"] = query.kind
    if query.module and query.module != "GENERAL":
        filters["target_module"] = query.module
    career_domain = infer_career_domain_from_occupation(query.occupation)
    if career_domain:
        filters["career_domain"] = career_domain
    return filters


def infer_scene_code_from_query(query: RetrievalQuery) -> str:
    kind = query.kind or ""
    if kind.startswith("interview_"):
        return "interview"
    if kind in {"keyword_map", "jd_profile"}:
        return "job_match"
    return "resume_rewrite"


def infer_career_domain_from_occupation(occupation: str | None) -> str | None:
    mapping = {
        "编程与 AI 应用": "software_ai",
        "硬件与嵌入式": "hardware_embedded",
        "教育与培训": "education_training",
        "产品与运营": "product_ops",
        "销售与市场": "sales_marketing",
        "设计与内容": "design_content",
        "财务行政与人力": "finance_admin_hr",
        "服务与医疗健康": "service_healthcare",
        "制造供应链与物流": "manufacturing_supply",
    }
    return mapping.get(occupation or "")


def to_reference_chunk(item: dict[str, Any]) -> ReferenceChunk:
    """把 rag_query 返回的 dict 转成现有图状态使用的 ReferenceChunk。"""
    metadata = dict(item)
    text = metadata.pop("text", "")
    source = metadata.pop("source", None)
    score = metadata.get("vector_score") or metadata.get("bm25_score") or metadata.get("score")
    return ReferenceChunk(text=text, source=source, score=score, metadata=metadata)


def collect_keywords(gap_report: dict[str, Any], jd_analysis: dict[str, Any]) -> list[str]:
    """从差距报告和 JD 分析里收集检索关键词。"""
    keywords = [
        *gap_report.get("missingKeywords", []),
        *jd_analysis.get("mustHaveKeywords", []),
        *jd_analysis.get("responsibilityKeywords", []),
    ]
    result = []
    for keyword in keywords:
        text = str(keyword).strip()
        if text and text not in result:
            result.append(text)
    return result[:10]


def infer_occupation(text: str) -> str | None:
    """根据 JD 关键词粗略判断职业画像，作为模型不可用时的兜底。"""
    lower_text = text.lower()
    occupation_keywords = [
        ("编程与 AI 应用", ["python", "java", "spring", "vue", "react", "rag", "agent", "ai", "算法", "开发"]),
        ("硬件与嵌入式", ["硬件", "嵌入式", "pcb", "mcu", "stm32", "电路", "驱动", "传感器"]),
        ("教育与培训", ["教师", "教学", "课程", "教案", "学生", "培训", "教研"]),
        ("产品与运营", ["产品", "运营", "用户", "增长", "活动", "社群", "prd"]),
        ("销售与市场", ["销售", "客户", "商务", "市场", "合同", "回款", "渠道"]),
        ("设计与内容", ["设计", "ui", "视觉", "品牌", "文案", "内容", "剪辑"]),
        ("财务行政与人力", ["财务", "会计", "行政", "人事", "招聘", "薪酬", "报销"]),
        ("服务与医疗健康", ["客服", "门店", "护士", "医疗", "患者", "护理", "健康"]),
        ("制造供应链与物流", ["制造", "供应链", "物流", "采购", "仓储", "质检", "生产"]),
    ]
    for occupation, keywords in occupation_keywords:
        if any(keyword in lower_text for keyword in keywords):
            return occupation
    return None


def get_target_section(state: ResumeAgentState) -> dict[str, Any]:
    """根据 target_section_id 找到本轮允许改写的简历模块。"""
    target_section_id = state.get("target_section_id")
    sections = state.get("resume_snapshot", {}).get("sections", [])
    for section in sections:
        section_id = section.get("id") or section.get("sectionId")
        if section_id == target_section_id:
            return section
    return {}


def deduplicate_chunks(chunks: list[ReferenceChunk]) -> list[ReferenceChunk]:
    """按来源和正文去重，避免多个 query 召回同一片段。"""
    result: list[ReferenceChunk] = []
    seen = set()
    for chunk in chunks:
        key = (chunk.source, chunk.metadata.get("chunkIndex"), chunk.text)
        if key in seen:
            continue
        seen.add(key)
        result.append(chunk)
    return result


def limit_chunks_by_chars(chunks: list[ReferenceChunk], max_chars: int) -> list[ReferenceChunk]:
    """控制塞给 Rewriter 的检索上下文长度。"""
    result: list[ReferenceChunk] = []
    total_chars = 0
    for chunk in chunks:
        text_len = len(chunk.text)
        if result and total_chars + text_len > max_chars:
            break
        result.append(chunk)
        total_chars += text_len
    return result


def normalize_retrieved_chunks(chunks: list[ReferenceChunk]) -> list[dict]:
    """规范化检索结果写入 state。"""
    return [chunk.model_dump() for chunk in chunks]
