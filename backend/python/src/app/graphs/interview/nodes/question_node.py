import json

from langchain_core.messages import HumanMessage, SystemMessage
from langgraph.types import interrupt

from app.core.llm import agent_factory
from app.graphs.interview.prompts import question_prompt
from app.graphs.interview.state import InterviewAgentState
from app.graphs.interview.types import InterviewQuestion
from app.schemas.internal.internal_interview_question_create_request import InternalInterviewQuestionCreateRequest
from app.services.java_gateway_service import java_gateway_service


async def question_node(state: InterviewAgentState) -> dict:
    context = {
        "resume_snapshot": state.get("resume_snapshot", {}),
        "job_description": state.get("job_description", ""),
        "summary": state.get("summary", ""),
        "current_round_no": state.get("current_round_no", 1),
        "question_history": state.get("question_history", []),
        "answer_history": state.get("answer_history", []),
        "evaluation_history": state.get("evaluation_history", []),
    }

    messages = [
        SystemMessage(content=question_prompt.QUESTION_SYSTEM_PROMPT),
        HumanMessage(content=json.dumps(context, ensure_ascii=False)),
    ]

    question = await agent_factory.invoke_agent(
        messages=messages,
        response_model=InterviewQuestion,
    )

    # 1. 先写 Java，拿到 round_id
    create_request = InternalInterviewQuestionCreateRequest(
        questionText=question.question_text,
        options=question.options,
    )
    create_response = await java_gateway_service.create_question_round(
        state["run_id"],
        create_request,
    )

    # 2. interrupt 暂停图, 这个等会可以取值(在graph里面的_extract_interrupt_payload这个方法用得到)
    interrupt({
        "round_id": create_response.round_id,
        "round_no": create_response.round_no,
        "question": question.model_dump(by_alias=True, exclude_none=True),
    })

    # 3. return 的 state 会被存入 checkpoint
    return {
        "current_round_id": create_response.round_id,
        "current_round_no": create_response.round_no,
        "current_question": question.model_dump(by_alias=True, exclude_none=True),
        "status": "WAITING_ANSWER",
        "current_stage": "QUESTION",
    }

