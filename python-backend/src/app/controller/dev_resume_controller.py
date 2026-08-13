import logging
from typing import Any
from uuid import uuid4

from fastapi import APIRouter
from langgraph.checkpoint.redis.aio import AsyncRedisSaver
from langgraph.types import Command
from pydantic import BaseModel, Field

from src.app.agent.graph import (
    logged_node,
    decide_memory_route,
    decide_supervisor_route,
    decide_review_route,
    get_skipped_nodes,
)
from src.app.agent.constants import AgentRoute
from src.app.agent.nodes.bootstrap_node import bootstrap_node
from src.app.agent.nodes.summarize_conversation_node import summarize_conversation_node
from src.app.agent.nodes.supervisor_node import supervisor_node
from src.app.agent.nodes.jd_analyst_node import jd_analyst_node
from src.app.agent.nodes.gap_analyzer_node import gap_analyzer_node
from src.app.agent.nodes.retriever_node import retriever_node
from src.app.agent.nodes.rewriter_node import rewriter_node
from src.app.agent.nodes.reviewer_node import reviewer_node
from src.app.agent.nodes.clarifier_node import clarifier_node
from src.app.agent.nodes.approval_packager_node import approval_packager_node
from src.app.agent.state import ResumeAgentState

router = APIRouter(prefix="/dev/resume", tags=["dev-resume"])
logger = logging.getLogger("uvicorn.error")

# ============================================================
# Dev 专用图: 使用 RedisSaver (持久化 checkpoint)
#
# 完整流程:
#   1. POST /dev/resume/optimize       → graph 跑到 interrupt → 保存 checkpoint → 返回 thread_id
#   2. POST /dev/resume/continue       → Command(resume=...) 从 checkpoint 恢复 → 继续执行
#
# checkpoint 保存在 Redis 里, 可在 RESP.app 里查看。
# 懒加载: 第一次请求时才连 Redis, 避免启动时 Redis 没就绪导致崩溃。
# ============================================================
_checkpointer = None
_dev_graph = None


async def _get_checkpointer():
    """懒加载 AsyncRedisSaver, 第一次调用时连接 Redis。"""
    global _checkpointer
    if _checkpointer is not None:
        return _checkpointer
    import redis.asyncio as aioredis
    client = aioredis.Redis(
        host="127.0.0.1",
        port=6379,
        db=0,
        socket_connect_timeout=5,
        socket_timeout=5,
        decode_responses=True,
    )
    _checkpointer = AsyncRedisSaver(redis_url="redis://127.0.0.1:6379/0", redis_client=client)
    await _checkpointer.asetup()
    logger.info("[resume-dev] AsyncRedisSaver connected")
    return _checkpointer


async def _build_dev_graph():
    """构建带 AsyncRedisSaver checkpointer 的图。"""
    from langgraph.graph import END, StateGraph
    from src.app.agent.state import ResumeAgentInput, ResumeAgentOutput

    workflow = StateGraph(
        state_schema=ResumeAgentState,
        input_schema=ResumeAgentInput,
        output_schema=ResumeAgentOutput,
    )
    workflow.add_node("bootstrap", logged_node("bootstrap", bootstrap_node))
    workflow.add_node("summarize_conversation", logged_node("summarize_conversation", summarize_conversation_node))
    workflow.add_node("supervisor", logged_node("supervisor", supervisor_node))
    workflow.add_node("jd_analyst", logged_node("jd_analyst", jd_analyst_node))
    workflow.add_node("gap_analyzer", logged_node("gap_analyzer", gap_analyzer_node))
    workflow.add_node("retriever", logged_node("retriever", retriever_node))
    workflow.add_node("rewriter", logged_node("rewriter", rewriter_node))
    workflow.add_node("reviewer", logged_node("reviewer", reviewer_node))
    workflow.add_node("clarifier", logged_node("clarifier", clarifier_node))
    workflow.add_node("approval_packager", logged_node("approval_packager", approval_packager_node))

    workflow.set_entry_point("bootstrap")
    workflow.add_conditional_edges("bootstrap", decide_memory_route, {
        "summarize_conversation": "summarize_conversation", "supervisor": "supervisor",
    })
    workflow.add_edge("summarize_conversation", "supervisor")
    workflow.add_conditional_edges("supervisor", decide_supervisor_route, {
        AgentRoute.JD_ANALYST: "jd_analyst", AgentRoute.CLARIFIER: "clarifier", AgentRoute.FAILED: END,
    })
    workflow.add_edge("jd_analyst", "gap_analyzer")
    workflow.add_edge("gap_analyzer", "retriever")
    workflow.add_edge("retriever", "rewriter")
    workflow.add_edge("rewriter", "reviewer")
    workflow.add_conditional_edges("reviewer", decide_review_route, {
        AgentRoute.REWRITER: "rewriter", AgentRoute.APPROVAL_PACKAGER: "approval_packager", AgentRoute.FAILED: END,
    })
    workflow.add_edge("clarifier", "supervisor")
    workflow.add_edge("approval_packager", END)
    return workflow.compile(checkpointer=await _get_checkpointer())


async def _get_dev_graph():
    """懒加载 dev 图, 第一次请求时才构建。"""
    global _dev_graph
    if _dev_graph is None:
        _dev_graph = await _build_dev_graph()
    return _dev_graph


# ============================================================
# 请求 / 响应 DTO
# ============================================================

class DevResumeOptimizeRequest(BaseModel):
    """启动 Agent run。"""
    run_id: int = Field(default=1, alias="runId")
    session_id: int = Field(default=1, alias="sessionId")
    resume_id: int = Field(default=1, alias="resumeId")
    scene_code: str = Field(default="JD_CUSTOMIZE", alias="sceneCode")
    user_input: str | None = Field(default=None, alias="userInput")
    job_description: str | None = Field(default=None, alias="jobDescription")
    target_section_id: int = Field(..., alias="targetSectionId")
    resume_snapshot: dict[str, Any] = Field(..., alias="resumeSnapshot")
    section_schemas: dict[str, dict[str, Any]] = Field(default_factory=dict, alias="sectionSchemas")
    history_messages: list[dict[str, Any]] = Field(default_factory=list, alias="historyMessages")
    summary: str = ""


class DevContinueRequest(BaseModel):
    """用户回答追问后继续执行。"""
    thread_id: str = Field(..., alias="threadId", description="LangGraph thread ID from first call")
    answers: list[dict[str, str]] = Field(default_factory=list, description="User answers [{fieldKey, value}]")
    approved_patches: list[str] = Field(default_factory=list, alias="approvedPatches", description="Patch IDs user approved")


# ============================================================
# 路由
# ============================================================

@router.post("/optimize")
async def optimize_resume(request: DevResumeOptimizeRequest) -> dict[str, Any]:
    """Step 1: 启动 Agent run, 跑到 interrupt 后返回 thread_id + payload。

    流程:
      graph.astream(state, config) → 逐节点执行
      → clarifier 调用 interrupt() → checkpoint 保存到 MemorySaver
      → 返回 {thread_id, clarificationPayload}
    """
    state = _build_state(request)
    thread_id = f"dev-{request.run_id}-{uuid4()}"
    config = {"configurable": {"thread_id": thread_id, "checkpoint_ns": "dev-resume"}}

    merged_state = dict(state)
    interrupt_payload = None

    async for update in (await _get_dev_graph()).astream(state, config=config, stream_mode="updates"):
        for node_name, node_state in update.items():
            if node_name == "__interrupt__":
                interrupt_payload = _extract_interrupt_value(node_state)
                logger.info("[resume-dev] interrupt thread=%s payload=%s", thread_id, interrupt_payload)
                continue
            if isinstance(node_state, dict):
                merged_state.update(node_state)
                executed = merged_state.get("executed_nodes", [])
                if node_name not in executed:
                    merged_state["executed_nodes"] = [*executed, node_name]

    resp = _build_response(merged_state)
    resp["threadId"] = thread_id

    # interrupt payload 按类型塞对应字段
    if interrupt_payload and isinstance(interrupt_payload, dict):
        if "questions" in interrupt_payload:
            resp["clarificationPayload"] = interrupt_payload
        elif "patches" in interrupt_payload or "summary" in interrupt_payload:
            resp["approvalPayload"] = interrupt_payload
        else:
            resp["interruptPayload"] = interrupt_payload

    logger.info(
        "[resume-dev] optimize done thread=%s executed=%s interrupt=%s",
        thread_id, merged_state.get("executed_nodes", []), interrupt_payload is not None,
    )
    return resp


@router.post("/continue")
async def continue_resume(request: DevContinueRequest) -> dict[str, Any]:
    """Step 2: 从 checkpoint 恢复, 用 Command(resume=...) 注入用户回答, 继续执行。

    流程:
      graph.astream(Command(resume={answers}), config) → 从 interrupt 点恢复
      → clarifier_node 拿到 resume_payload → 写入 state["clarification_answers"]
      → graph 继续走向 supervisor → jd_analyst → ... → interrupt 或 END
    """
    config = {"configurable": {"thread_id": request.thread_id, "checkpoint_ns": "dev-resume"}}

    # 构造 resume payload, 和 clarifier_node 里 interrupt() 的返回值格式一致
    resume_payload = {"answers": request.answers}

    merged_state: dict[str, Any] = {}
    interrupt_payload = None

    async for update in (await _get_dev_graph()).astream(
        Command(resume=resume_payload),
        config=config,
        stream_mode="updates",
    ):
        for node_name, node_state in update.items():
            if node_name == "__interrupt__":
                interrupt_payload = _extract_interrupt_value(node_state)
                logger.info("[resume-dev] interrupt thread=%s payload=%s", request.thread_id, interrupt_payload)
                continue
            if isinstance(node_state, dict):
                merged_state.update(node_state)
                executed = merged_state.get("executed_nodes", [])
                if node_name not in executed:
                    merged_state["executed_nodes"] = [*executed, node_name]

    resp = _build_response(merged_state)
    resp["threadId"] = request.thread_id

    if interrupt_payload and isinstance(interrupt_payload, dict):
        if "questions" in interrupt_payload:
            resp["clarificationPayload"] = interrupt_payload
        elif "patches" in interrupt_payload or "summary" in interrupt_payload:
            resp["approvalPayload"] = interrupt_payload
        else:
            resp["interruptPayload"] = interrupt_payload

    logger.info(
        "[resume-dev] continue done thread=%s executed=%s interrupt=%s",
        request.thread_id, merged_state.get("executed_nodes", []), interrupt_payload is not None,
    )
    return resp


# ============================================================
# 工具函数
# ============================================================

def _build_state(request: DevResumeOptimizeRequest) -> ResumeAgentState:
    return {
        "run_id": request.run_id,
        "session_id": request.session_id,
        "resume_id": request.resume_id,
        "scene_code": request.scene_code,
        "user_input": request.user_input,
        "job_description": request.job_description,
        "target_section_id": request.target_section_id,
        "resume_snapshot": request.resume_snapshot,
        "section_schemas": request.section_schemas,
        "history_messages": request.history_messages,
        "summary": request.summary,
        "clarification_answers": [],
        "editable_section_ids": [request.target_section_id],
        "constraints": {},
        "review_retry_count": 0,
        "event_seq": 0,
        "errors": [],
    }


def _build_response(result: dict[str, Any]) -> dict[str, Any]:
    return {
        "runId": result.get("run_id"),
        "status": result.get("status"),
        "currentStage": result.get("current_stage"),
        "approvalPayload": result.get("approval_payload"),
        "clarificationPayload": result.get("clarification_payload"),
        "candidatePatches": result.get("candidate_patches", []),
        "reviewResult": result.get("review_result"),
        "retrievalError": result.get("retrieval_error"),
        "executedNodes": result.get("executed_nodes", []),
        "skippedNodes": get_skipped_nodes(result.get("executed_nodes", [])),
        "errors": result.get("errors", []),
    }


def _extract_interrupt_value(node_state: Any) -> Any:
    if isinstance(node_state, (list, tuple)) and node_state:
        first = node_state[0]
        value = getattr(first, "value", None)
        return value if value is not None else first
    value = getattr(node_state, "value", None)
    if value is not None:
        return value
    if isinstance(node_state, dict):
        return node_state.get("value", node_state)
    return node_state
