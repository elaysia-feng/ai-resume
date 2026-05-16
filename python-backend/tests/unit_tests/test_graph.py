import asyncio

from langchain_core.messages import AIMessage, HumanMessage

from src.agent.graph import graph


async def raise_llm_unavailable(*args, **kwargs):
    """单元测试不调用真实 LLM，直接走节点降级逻辑。"""
    raise RuntimeError("mock llm unavailable")


class DummySummaryModel:
    """测试用摘要模型，避免调用真实 LLM。"""

    async def ainvoke(self, messages):
        return AIMessage(content="用户希望根据 Python/FastAPI/LangGraph 岗位优化简历。")


def test_graph_reaches_approval_without_java_bootstrap(monkeypatch):
    """有 JD 且传入 mock 简历时，主链路应能走到审批挂起。"""
    monkeypatch.setattr("src.app.agent.nodes.jd_analyst_node.agent_factory.invoke_agent", raise_llm_unavailable)
    monkeypatch.setattr("src.app.agent.nodes.gap_analyzer_node.agent_factory.invoke_agent", raise_llm_unavailable)
    monkeypatch.setattr("src.app.agent.nodes.retriever_node.agent_factory.invoke_agent", raise_llm_unavailable)

    result = asyncio.run(
        graph.ainvoke(
            {
                "run_id": 1,
                "session_id": 1,
                "resume_id": 1,
                "scene_code": "JD_CUSTOMIZE",
                "job_description": "Python FastAPI LangGraph",
                "target_section_id": 1,
                "resume_snapshot": {"id": 1, "title": "debug", "sections": []},
                "section_schemas": {},
                "history_messages": [],
            }
        )
    )

    assert result["status"] == "WAITING_CONFIRM"
    assert result["current_stage"] == "APPROVAL_PACKAGER"


def test_graph_reaches_clarifier_when_jd_missing(monkeypatch):
    """JD 定制场景缺少 JD 时，应进入追问挂起。"""
    monkeypatch.setattr("src.app.agent.nodes.clarifier_node.agent_factory.invoke_agent", raise_llm_unavailable)

    result = asyncio.run(
        graph.ainvoke(
            {
                "run_id": 2,
                "session_id": 1,
                "resume_id": 1,
                "scene_code": "JD_CUSTOMIZE",
                "target_section_id": 1,
                "resume_snapshot": {"id": 1, "title": "debug", "sections": []},
                "section_schemas": {},
                "history_messages": [],
            }
        )
    )

    assert result["__interrupt__"]
    assert result["__interrupt__"][0].value["questions"]


def test_graph_summarizes_old_messages(monkeypatch):
    """messages 过多时，应写入 summary 并只保留最近消息。"""
    monkeypatch.setattr(
        "src.app.agent.memory.agent_factory.create_chat_model",
        lambda: DummySummaryModel(),
    )
    monkeypatch.setattr("src.app.agent.nodes.jd_analyst_node.agent_factory.invoke_agent", raise_llm_unavailable)
    monkeypatch.setattr("src.app.agent.nodes.gap_analyzer_node.agent_factory.invoke_agent", raise_llm_unavailable)
    monkeypatch.setattr("src.app.agent.nodes.retriever_node.agent_factory.invoke_agent", raise_llm_unavailable)

    result = asyncio.run(
        graph.ainvoke(
            {
                "run_id": 3,
                "session_id": 1,
                "resume_id": 1,
                "scene_code": "JD_CUSTOMIZE",
                "job_description": "Python FastAPI LangGraph",
                "target_section_id": 1,
                "resume_snapshot": {"id": 1, "title": "debug", "sections": []},
                "section_schemas": {},
                "history_messages": [],
                "messages": [HumanMessage(content=f"历史消息 {index}", id=f"m-{index}") for index in range(13)],
            }
        )
    )

    assert result["summary"] == "用户希望根据 Python/FastAPI/LangGraph 岗位优化简历。"
    assert [message.id for message in result["messages"]] == [f"m-{index}" for index in range(7, 13)]
