from typing import Any
from langgraph.graph import MessagesState


class InterviewAgentState(MessagesState, total=False):
    # 运行标识
    run_id: int
    session_id: int
    resume_id: int
    scene_code: str

    # 输入上下文
    resume_snapshot: dict[str, Any]
    job_description: str
    summary: str
    interview_context: dict[str, Any] | None


    # 当前轮次
    current_round_id: int | None
    current_round_no: int
    current_question: dict[str, Any] | None

    # 历史记录
    question_history: list[dict[str, Any]]
    answer_history: list[dict[str, Any]]
    evaluation_history: list[dict[str, Any]]

    # 运行控制
    status: str
    current_stage: str
    event_seq: int
    error: str | None
    has_next_question: bool | None
