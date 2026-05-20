from langchain_core.messages import SystemMessage, HumanMessage
import json

from src.app.interview.prompts import analyst_answer_prompt
from src.app.interview.state import InterviewAgentState
from src.app.interview.types import InterviewEvaluation
from src.app.service.agent_factory import agent_factory

MAX_ROUNDS = 5


async def analyst_answer_node(state: InterviewAgentState) -> dict:
    state = dict(state)
    state.pop("messages", None)
    state["current_stage"] = "ANALYST_ANSWER"

    current_round_no = state.get("current_round_no", 0)
    current_question = state.get("current_question")
    answer_history = state.get("answer_history", [])
    current_answer = answer_history[-1] if answer_history else None

    context = {
        "current_round_no": current_round_no,
        "max_rounds": MAX_ROUNDS,
        "current_question": current_question,
        "current_answer": current_answer,
        "evaluation_history": state.get("evaluation_history", []),
        "job_description": state.get("job_description"),
    }

    messages = [
        SystemMessage(content=analyst_answer_prompt.ANALYST_ANSWER_SYSTEM_PROMPT),
        HumanMessage(content=json.dumps(context, ensure_ascii=False)),
    ]

    result = await agent_factory.invoke_agent(
        messages=messages,
        response_model=InterviewEvaluation,
    )

    evaluation_history = list(state.get("evaluation_history", []))
    evaluation_history.append({
        "round_no": current_round_no,
        "evaluation": result.evaluation,
        "score": result.score,
    })

    has_next = result.has_next_question and current_round_no < MAX_ROUNDS

    return {
        "evaluation_history": evaluation_history,
        "has_next_question": has_next,
    }
