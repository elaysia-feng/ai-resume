from app.interview.state import InterviewAgentState


def route_after_analysis(state: InterviewAgentState) -> str:
    """决定是否结束面试,进入总结环节"""
    if state.get("has_next_question"):
        return "question"
    return "summary"