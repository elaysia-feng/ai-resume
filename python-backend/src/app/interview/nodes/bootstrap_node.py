from src.app.internal_dto.interview_bootstrap_request import InterviewBootstrapRequest
from src.app.internal_dto.interview_bootstrap_response import InterviewBootstrapResponse
from src.app.interview.state import InterviewAgentState
from src.app.service.java_gateway_service import java_gateway_service


# 从 java 加载上下文
async def bootstrap_node(state: InterviewAgentState) -> dict:
    state = dict(state)
    state["status"] = "RUNNING"
    state["current_stage"] = "BOOTSTRAP"
    state.setdefault("scene_code", "INTERVIEW")
    state.setdefault("event_seq", 0)
    state.setdefault("error", None)
    state.setdefault("current_round_id", None)
    state.setdefault("current_round_no", 0)
    state.setdefault("current_question", None)
    state.setdefault("question_history", [])
    state.setdefault("answer_history", [])
    state.setdefault("evaluation_history", [])

    # 从 Java 加载简历、JD 等上下文
    request = build_bootstrap_request(state)
    response = await java_gateway_service.load_interview_bootstrap_context(request)
    apply_bootstrap_response(state, response)

    state.pop("messages", None)
    return state

def build_bootstrap_request(state: InterviewAgentState) -> InterviewBootstrapRequest:
    """根据 state 构造 Java bootstrap 请求。"""
    return InterviewBootstrapRequest(
        runId=state["run_id"],
        sessionId=state["session_id"],
        resumeId=state["resume_id"],
        sceneCode=state["scene_code"],
    )

def apply_bootstrap_response(state: InterviewAgentState, response: InterviewBootstrapResponse) -> dict:
    """把 Java 返回的结果写回 State。"""
    state["run_id"] = response.run_id
    state["session_id"] = response.session_id
    if response.resume_id is not None:
        state["resume_id"] = response.resume_id
    state["resume_snapshot"] = response.resume.model_dump(by_alias=True)
    if response.job_description and not state.get("job_description"):
        state["job_description"] = response.job_description

    if response.summary and not state.get("summary"):
        state["summary"] = response.summary

    return state
