from langchain_core.messages import HumanMessage, SystemMessage

from src.app.agent.constants import AgentRoute, AgentStage
from src.app.agent.memory import build_memory_messages, compact_memory_for_model
from src.app.agent.prompts.supervisor_prompt import SUPERVISOR_SYSTEM_PROMPT
from src.app.agent.state import ResumeAgentState
from src.app.agent.types import AgentRouteDecision
from src.app.service.agent_factory import agent_factory
"""
普通字段：赋值/return = 覆盖写入
messages：return {"messages": [...]} = 自动追加
list 普通字段：如果想追加，需要自己 append 或拼接后返回
"""

async def supervisor_node(state: ResumeAgentState) -> ResumeAgentState:
    """Supervisor Agent：判断任务类型、缺失信息和下一步路由。"""
    input_state = dict(state)
    state = dict(state)
    # 本节点只产出 route_decision，不新增对话消息；移除 messages 可避免重复写回。
    state.pop("messages", None)
    state["current_stage"] = AgentStage.SUPERVISOR

    try:
        # 正常链路交给结构化 Agent 判断，避免只看 job_description 字段导致误判。
        messages = build_supervisor_messages(input_state)
        memory_updates, input_state = await compact_memory_for_model(input_state, messages)
        state.update(memory_updates)
        decision = await agent_factory.invoke_agent(build_supervisor_messages(input_state), AgentRouteDecision)
        decision = normalize_route_decision(decision)
    except Exception:
        # 模型未配置或调用失败时，用本地规则兜底，保证 Studio/单测可以继续跑。
        decision = build_fallback_route_decision(input_state)

    state["route_decision"] = decision.model_dump(by_alias=True)
    return state


def build_supervisor_messages(state: ResumeAgentState) -> list:
    """构造 Supervisor Agent 的模型消息。"""
    return [
        SystemMessage(content=SUPERVISOR_SYSTEM_PROMPT),
        # 带上摘要和最近消息，让 Supervisor 能识别写在 user_input 或历史消息中的 JD。
        *build_memory_messages(state),
        HumanMessage(
            content=(
                f"场景：{state.get('scene_code') or ''}\n"
                f"目标模块ID：{state.get('target_section_id') or ''}\n"
                f"用户输入：{state.get('user_input') or ''}\n"
                f"岗位 JD 字段：{state.get('job_description') or ''}\n"
                f"用户补充答案：{state.get('clarification_answers') or []}"
            )
        ),
    ]


def parse_route_decision(raw_response: object) -> AgentRouteDecision:
    """解析 Supervisor Agent 路由结果。"""
    return AgentRouteDecision.model_validate(raw_response)


def normalize_route_decision(decision: AgentRouteDecision) -> AgentRouteDecision:
    """规范化模型返回的路由，避免未知节点进入 conditional_edges。"""
    next_node = str(decision.next_node or "").strip().lower()
    if next_node not in {AgentRoute.JD_ANALYST, AgentRoute.CLARIFIER}:
        return AgentRouteDecision(
            nextNode=AgentRoute.CLARIFIER,
            reason="Supervisor 返回了未知路由，需要先追问确认目标岗位信息",
            clarificationNeeded=True,
        )
    return AgentRouteDecision(
        nextNode=next_node,
        reason=decision.reason,
        clarificationNeeded=next_node == AgentRoute.CLARIFIER or decision.clarification_needed,
    )


def build_fallback_route_decision(state: ResumeAgentState) -> AgentRouteDecision:
    """模型不可用时的本地兜底路由，保证调试环境能继续运行。"""
    if should_enter_clarifier(state):
        return AgentRouteDecision(
            nextNode=AgentRoute.CLARIFIER,
            reason="缺少岗位 JD 或关键补充信息",
            clarificationNeeded=True,
        )
    return AgentRouteDecision(
        nextNode=AgentRoute.JD_ANALYST,
        reason="上下文足够，进入 JD 分析",
        clarificationNeeded=False,
    )


def should_enter_clarifier(state: ResumeAgentState) -> bool:
    """判断是否需要进入 Clarifier Agent。"""
    scene_code = state.get("scene_code")
    user_input = str(state.get("user_input") or "").strip()
    has_target_section = bool(state.get("target_section_id"))
    has_jd = bool(state.get("job_description")) or looks_like_jd_text(user_input)
    has_answers = bool(state.get("clarification_answers"))
    return (not has_target_section) or (scene_code == "JD_CUSTOMIZE" and not has_jd and not has_answers)


def looks_like_jd_text(text: str) -> bool:
    """轻量判断 user_input 中是否直接包含岗位信息。"""
    if not text:
        return False
    jd_keywords = [
        "岗位",
        "职位",
        "职责",
        "要求",
        "任职",
        "招聘",
        "熟悉",
        "负责",
        "经验",
        "spring",
        "mysql",
        "redis",
    ]
    lower_text = text.lower()
    # 命中两个以上 JD 关键词时，认为用户输入大概率已经包含岗位信息。
    return sum(1 for keyword in jd_keywords if keyword in lower_text) >= 2
