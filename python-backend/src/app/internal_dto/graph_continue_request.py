from pydantic import BaseModel, Field

from src.app.internal_dto.clarification_answer import ClarificationAnswer


class GraphContinueRequest(BaseModel):
    """Java 调 Python 继续 Agent run 的请求。"""

    run_id: int | None = Field(default=None, alias="runId", description="Java 侧 ai_agent_run 主键")
    answers: list[ClarificationAnswer] = Field(default_factory=list, description="用户补充答案")
