from src.app.agent.state import ResumeAgentState
from src.app.agent.constants import AgentStage, AgentStatus
from src.app.agent.memory import build_initial_messages
from src.app.service.java_gateway_service import java_gateway_service
from src.app.internal_dto.bootstrap_request import BootstrapRequest
from src.app.internal_dto.bootstrap_response import BootstrapResponse


async def bootstrap_node(state: ResumeAgentState) -> ResumeAgentState:
    """Bootstrap Agent：加载简历、会话、schema 等上下文。"""
    state = dict(state)
    has_messages = bool(state.get("messages"))
    state["status"] = AgentStatus.RUNNING
    state["current_stage"] = AgentStage.BOOTSTRAP
    state.setdefault("event_seq", 0)
    state.setdefault("review_retry_count", 0)
    state.setdefault("errors", [])

    if state.get("resume_snapshot"):
        # Studio 调试时可以直接传 resume_snapshot，这样不依赖 Java 后端也能跑图。
        state.setdefault("section_schemas", {})
        state.setdefault("history_messages", [])
        state.setdefault("editable_section_ids", [])
        state.setdefault("constraints", {})
        apply_initial_messages(state, has_messages)
        return state

    # 正常 API 链路只带 run/session/resume 等 ID，完整业务上下文由 Java 内部接口提供。
    response = await java_gateway_service.load_bootstrap_context(build_bootstrap_request(state))
    apply_bootstrap_response(state, response)
    apply_initial_messages(state, has_messages)
    return state


def apply_initial_messages(state: ResumeAgentState, has_messages: bool) -> None:
    """首次启动时把 Java 历史和本轮输入写入 MessagesState。"""
    if has_messages:
        # 当前节点不新增消息时不要把原 messages 原样返回，否则可能触发重复合并。
        state.pop("messages", None)
        return

    messages = build_initial_messages(state)
    if messages:
        # 返回 messages 后，MessagesState reducer 会把它们追加进图状态。
        state["messages"] = messages
    else:
        state.pop("messages", None)


def build_bootstrap_request(state: ResumeAgentState) -> BootstrapRequest:
    """根据 state 构造 Java bootstrap 请求。"""
    return BootstrapRequest(
        runId=state["run_id"],
        sessionId=state["session_id"],
        resumeId=state["resume_id"],
        sceneCode=state["scene_code"],
        targetSectionId=state["target_section_id"],
    )


def apply_bootstrap_response(state: ResumeAgentState, response: BootstrapResponse) -> ResumeAgentState:
    """把 Java bootstrap 响应写回 state。"""
    state["resume_snapshot"] = response.resume.model_dump(by_alias=True)
    if response.job_description and not state.get("job_description"):
        # JD 属于 session 长期上下文；run 没传时复用 Java session 中保存的 JD。
        state["job_description"] = response.job_description
    if response.summary and not state.get("summary"):
        # summary 由 Java 持久化，Python 每轮 bootstrap 时带入模型上下文。
        state["summary"] = response.summary
    state["section_schemas"] = response.schemas
    state["history_messages"] = [message.model_dump(by_alias=True) for message in response.messages]
    state["editable_section_ids"] = response.editable_section_ids
    state["constraints"] = response.constraints.model_dump(by_alias=True)
    return state


def next_event_seq(state: ResumeAgentState) -> int:
    """获取下一条 event 序号。"""
    state["event_seq"] = state.get("event_seq", 0) + 1
    return state["event_seq"]
