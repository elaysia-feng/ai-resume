import re

from langchain_core.messages import HumanMessage, SystemMessage

from src.app.agent.constants import AgentStage
from src.app.agent.memory import build_memory_messages, compact_memory_for_model
from src.app.agent.prompts.jd_analyst_prompt import JD_ANALYST_SYSTEM_PROMPT
from src.app.agent.state import ResumeAgentState
from src.app.agent.types import JdAnalysis
from src.app.service.agent_factory import agent_factory


async def jd_analyst_node(state: ResumeAgentState) -> ResumeAgentState:
    """JdAnalyst Agent：解析岗位 JD，提取岗位要求。"""
    input_state = dict(state)
    state = dict(state)
    # 本节点只写 jd_analysis，不向 messages 追加新对话。
    state.pop("messages", None)
    state["current_stage"] = AgentStage.JD_ANALYST
    text = state.get("job_description") or state.get("user_input") or ""
    try:
        # 结构化输出能让后续 gap_analyzer / retriever 稳定读取关键词字段。
        messages = build_jd_analyst_messages(input_state)
        memory_updates, input_state = await compact_memory_for_model(input_state, messages)
        state.update(memory_updates)
        analysis = await agent_factory.invoke_agent(build_jd_analyst_messages(input_state), JdAnalysis)
    except Exception:
        # 本地降级保证没有模型配置时也能在 Studio 看完整图流转。
        analysis = build_fallback_jd_analysis(text, state)
    state["jd_analysis"] = analysis.model_dump(by_alias=True)
    return state


def build_jd_analyst_messages(state: ResumeAgentState) -> list:
    """构造 JdAnalyst Agent 的模型消息。"""
    return [
        SystemMessage(content=JD_ANALYST_SYSTEM_PROMPT),
        *build_memory_messages(state),
        HumanMessage(content=str(state.get("job_description") or state.get("user_input") or "")),
    ]


def parse_jd_analysis(raw_response: object) -> JdAnalysis:
    """解析 JD 分析结果。"""
    return JdAnalysis.model_validate(raw_response)


def _extract_keywords(text: str) -> list[str]:
    """从文本中提取用于调试的轻量关键词。"""
    words = re.split(r"[\s,，、;；/｜|（）()]+", text)
    return [word.strip() for word in words if 1 < len(word.strip()) <= 30][:16]


def build_fallback_jd_analysis(text: str, state: ResumeAgentState) -> JdAnalysis:
    """模型不可用时的本地降级分析，保证 Studio 可以继续调试图。"""
    keywords = _extract_keywords(text)
    return JdAnalysis(
        targetPosition=keywords[0] if keywords else None,
        mustHaveKeywords=keywords[:8],
        niceToHaveKeywords=keywords[8:12],
        responsibilityKeywords=keywords[:8],
        deliverableKeywords=[],
        prioritySections=[str(state.get("target_section_id"))] if state.get("target_section_id") else [],
    )
