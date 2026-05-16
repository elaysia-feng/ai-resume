import asyncio

from src.app.agent.nodes.retriever_node import build_fallback_retrieval_plan, retriever_node
from src.app.agent.types import ReferenceChunk


async def raise_llm_unavailable(*args, **kwargs):
    """单元测试不调用真实 LLM，直接走 Retriever 兜底计划。"""
    raise RuntimeError("mock llm unavailable")


async def fake_search_reference_chunks_tool(*args, **kwargs):
    """测试用检索工具，避免连接真实 embedding 模型和 Qdrant。"""
    query = args[0]
    return [
        ReferenceChunk(
            text=f"参考片段: {query}",
            source="unit-test",
            score=0.9,
            metadata={"chunkIndex": 0, "query": query},
        )
    ]


def test_fallback_plan_infers_software_occupation():
    """兜底计划应能根据 JD 关键词选择职业画像。"""
    plan = build_fallback_retrieval_plan(
        {
            "job_description": "Python FastAPI RAG Agent 后端开发",
            "jd_analysis": {"targetPosition": "AI 应用工程师", "mustHaveKeywords": ["RAG", "FastAPI"]},
            "gap_report": {"missingKeywords": ["LangGraph"]},
            "resume_snapshot": {"sections": [{"id": 1, "sectionCode": "PROJECTS"}]},
            "target_section_id": 1,
        }
    )

    assert plan.should_retrieve is True
    assert plan.queries[0].occupation == "编程与 AI 应用"
    assert plan.queries[0].kind == "occupation_profile"


def test_retriever_node_writes_retrieved_chunks(monkeypatch):
    """Retriever 节点应执行检索计划，并把结果写回 state。"""
    monkeypatch.setattr("src.app.agent.nodes.retriever_node.agent_factory.invoke_agent", raise_llm_unavailable)
    monkeypatch.setattr(
        "src.app.agent.nodes.retriever_node.search_reference_chunks_tool",
        fake_search_reference_chunks_tool,
    )

    result = asyncio.run(
        retriever_node(
            {
                "job_description": "Python FastAPI RAG Agent 后端开发",
                "jd_analysis": {"targetPosition": "AI 应用工程师", "mustHaveKeywords": ["RAG", "FastAPI"]},
                "gap_report": {"missingKeywords": ["LangGraph"]},
                "resume_snapshot": {"sections": [{"id": 1, "sectionCode": "PROJECTS"}]},
                "target_section_id": 1,
            }
        )
    )

    assert result["current_stage"] == "RETRIEVER"
    assert result["retrieval_plan"]["shouldRetrieve"] is True
    assert result["retrieved_chunks"]
