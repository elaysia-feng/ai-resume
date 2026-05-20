from langchain_core.messages import SystemMessage, HumanMessage
import json

from src.app.interview.prompts import analyst_prompt
from src.app.interview.state import InterviewAgentState
from src.app.interview.types import InterviewContext
from src.app.service.agent_factory import agent_factory


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
