import json

from langchain_core.messages import HumanMessage, SystemMessage

from app.core.llm import agent_factory
from app.graphs.interview.prompts import analyst_prompt
from app.graphs.interview.state import InterviewAgentState
from app.graphs.interview.types import InterviewContext


async def analyst_node(state: InterviewAgentState) -> dict:
    state = dict(state)
    state.pop("messages", None)
    state["current_stage"] = "ANALYST"

    messages = [
        SystemMessage(content=analyst_prompt.SUPERVISOR_SYSTEM_PROMPT),
        HumanMessage(content=json.dumps({
            "resume_snapshot": state.get("resume_snapshot", {}),
            "job_description": state.get("job_description", ""),
        }, ensure_ascii=False)),
    ]

    result = await agent_factory.invoke_agent(
        messages=messages,
        response_model=InterviewContext,
    )

    return {
        "interview_context": result.model_dump(by_alias=True),
        "current_stage": "ANALYST",
    }
