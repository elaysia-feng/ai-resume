from src.app.interview.state import InterviewAgentState
from src.app.service.java_gateway_service import java_gateway_service


async def answer_node(state: InterviewAgentState) -> dict:
    state = dict(state)
    state.pop("messages", None)
    state["current_stage"] = "ANSWER"

    round_id = state.get("current_round_id")
    if not round_id:
        state["error"] = "current_round_id is missing"
        return state

    response = await java_gateway_service.get_question_answer(round_id)

    answer = {
        "question_id": round_id,
        "user_answer": response.user_answer,
    }

    answer_history = list(state.get("answer_history", []))
    answer_history.append(answer)

    return {
        "current_stage": "ANSWER",
        "answer_history": answer_history,
    }
