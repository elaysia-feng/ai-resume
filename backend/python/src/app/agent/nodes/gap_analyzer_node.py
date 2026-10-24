import json

from langchain_core.messages import HumanMessage, SystemMessage

from src.app.agent.constants import AgentStage
from src.app.agent.memory import build_memory_messages, compact_memory_for_model
from src.app.agent.prompts.gap_analyzer_prompt import GAP_ANALYZER_SYSTEM_PROMPT
from src.app.agent.state import ResumeAgentState
from src.app.agent.types import GapReport
from src.app.service.agent_factory import agent_factory


async def gap_analyzer_node(state: ResumeAgentState) -> ResumeAgentState:
    """GapAnalyzer Agent：对比简历与 JD，生成差距分析。"""
    input_state = dict(state)
    state = dict(state)
    # 只产出 gap_report，不修改对话历史。
    state.pop("messages", None)
    state["current_stage"] = AgentStage.GAP_ANALYZER
    try:
        messages = build_gap_analyzer_messages(input_state)
        memory_updates, input_state = await compact_memory_for_model(input_state, messages)
        state.update(memory_updates)
        report = await agent_factory.invoke_agent(build_gap_analyzer_messages(input_state), GapReport)
    except Exception:
        # 降级逻辑用关键词是否出现在简历 JSON 中做粗略匹配，方便本地无模型调试。
        report = build_fallback_gap_report(state)
    state["gap_report"] = report.model_dump(by_alias=True)
    return state


def build_fallback_gap_report(state: ResumeAgentState) -> GapReport:
    """模型不可用时的本地降级差距分析。"""
    jd_analysis = state.get("jd_analysis", {})
    keywords = jd_analysis.get("mustHaveKeywords", [])
    # 简单把简历快照序列化为文本做包含判断，不作为真实质量评估。
    resume_text = json.dumps(state.get("resume_snapshot", {}), ensure_ascii=False)
    matched = [keyword for keyword in keywords if keyword and keyword in resume_text]
    missing = [keyword for keyword in keywords if keyword and keyword not in resume_text]
    report = GapReport(
        matchedKeywords=matched,
        missingKeywords=missing,
        prioritySections=jd_analysis.get("prioritySections", []),
        gapSummary="已完成简历与 JD 的占位差距分析",
        details=[],
    )
    return report


def build_gap_analyzer_messages(state: ResumeAgentState) -> list:
    """构造 GapAnalyzer Agent 的模型消息。"""
    return [
        SystemMessage(content=GAP_ANALYZER_SYSTEM_PROMPT),
        *build_memory_messages(state),
        HumanMessage(
            content=(
                f"JD 分析：{json.dumps(state.get('jd_analysis', {}), ensure_ascii=False)}\n"
                f"简历快照：{json.dumps(state.get('resume_snapshot', {}), ensure_ascii=False)}"
            )
        ),
    ]


def parse_gap_report(raw_response: object) -> GapReport:
    """解析差距分析结果。"""
    return GapReport.model_validate(raw_response)
