import json

from langchain_core.messages import HumanMessage, SystemMessage

from app.core.llm import agent_factory
from app.graphs.interview.prompts import summary_prompt
from app.graphs.interview.state import InterviewAgentState
from app.graphs.interview.types import InterviewSummary


async def summary_node(state: InterviewAgentState) -> dict:
    state = dict(state)
    state.pop("messages", None)
    state["current_stage"] = "SUMMARY"
    state["status"] = "SUCCESS"

    input_message = {
        "resume_snapshot": state["resume_snapshot"],
        "job_description": state["job_description"],
        "summary": state["summary"],
        "question_history": state["question_history"],
        "answer_history": state["answer_history"],
        "evaluation_history": state["evaluation_history"],
    }

    context = [
        SystemMessage(content=summary_prompt.SUMMARY_SYSTEM_PROMPT),
        HumanMessage(content=json.dumps(input_message, ensure_ascii=False))
    ]


    response = await agent_factory.invoke_agent(
        messages=context,
        response_model=InterviewSummary,
    )

    return {
        "summary": response.summary,
        "highlight": response.highlight,
        "improvement": response.improvement,
        "score": response.score,
    }
