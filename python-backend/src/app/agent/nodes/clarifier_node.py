from langchain_core.messages import HumanMessage, SystemMessage
from langgraph.types import interrupt

from src.app.agent.constants import AgentStage, AgentStatus
from src.app.agent.memory import build_memory_messages, compact_memory_for_model
from src.app.agent.prompts.clarifier_prompt import CLARIFIER_SYSTEM_PROMPT
from src.app.agent.state import ResumeAgentState
from src.app.agent.types import ClarificationPayload, ClarificationQuestion
from src.app.service.agent_factory import agent_factory


async def clarifier_node(state: ResumeAgentState) -> ResumeAgentState:
    """Clarifier Agent：生成追问并用 LangGraph interrupt 挂起 run。"""
    input_state = dict(state)
    state = dict(state)
    # MessagesState 的 messages 由 reducer 维护；不新增消息时不要原样返回。
    state.pop("messages", None)
    state["current_stage"] = AgentStage.CLARIFIER
    state["status"] = AgentStatus.WAITING_USER
    try:
        # 追问也交给结构化 Agent，后续可以根据不同缺失字段生成不同问题。
        messages = build_clarifier_messages(input_state)
        memory_updates, input_state = await compact_memory_for_model(input_state, messages)
        state.update(memory_updates)
        payload = await agent_factory.invoke_agent(build_clarifier_messages(input_state), ClarificationPayload)
    except Exception:
        # 模型不可用时至少追问 JD，避免流程卡死。
        payload = parse_clarification_payload({}, state["run_id"])
    state["clarification_payload"] = payload.model_dump(by_alias=True)
    # 可以阻断对话, 如果Agent加入了checkpointer就在这个里面去恢复(也就是state是存入checkpointer的),反之存入内存
    resume_payload = interrupt(state["clarification_payload"])
    answers = extract_resume_answers(resume_payload)
    state["clarification_answers"] = answers
    for answer in answers:
        if answer.get("fieldKey") in {"jobDescription", "job_description", "jd"}:
            state["job_description"] = answer.get("value")
    state["status"] = AgentStatus.RUNNING
    return state


def extract_resume_answers(resume_payload: object) -> list[dict]:
    """从 Command(resume=...) 的 payload 中取出用户追问答案。"""
    if isinstance(resume_payload, dict):
        answers = resume_payload.get("answers")
        return answers if isinstance(answers, list) else []
    return []


def build_clarifier_messages(state: ResumeAgentState) -> list:
    """构造 Clarifier Agent 的模型消息。"""
    return [
        SystemMessage(content=CLARIFIER_SYSTEM_PROMPT),
        *build_memory_messages(state),
        HumanMessage(
            content=(
                f"runId：{state.get('run_id')}\n"
                f"目标模块ID：{state.get('target_section_id') or ''}\n"
                f"用户输入：{state.get('user_input') or ''}\n"
                f"场景：{state.get('scene_code') or ''}\n"
                f"当前缺失：{build_missing_fields_text(state)}"
            )
        ),
    ]


def parse_clarification_payload(raw_response: object, run_id: int) -> ClarificationPayload:
    """解析追问 payload。"""
    if isinstance(raw_response, dict) and raw_response.get("questions"):
        return ClarificationPayload.model_validate({"runId": run_id, "questions": raw_response["questions"]})
    return ClarificationPayload(
        runId=run_id,
        questions=[
            ClarificationQuestion(
                fieldKey="jobDescription",
                question="请补充目标岗位 JD 或核心招聘要求。",
            )
        ],
    )


def build_missing_fields_text(state: ResumeAgentState) -> str:
    """列出当前阻断执行的关键字段。"""
    missing = []
    if not state.get("target_section_id"):
        missing.append("targetSectionId")
    if not state.get("job_description"):
        missing.append("jobDescription")
    return "、".join(missing) or "无"
