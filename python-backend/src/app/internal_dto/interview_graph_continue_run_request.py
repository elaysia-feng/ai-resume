from typing import Literal

from pydantic import BaseModel, Field

from src.app.internal_dto.clarification_answer import ClarificationAnswer


class InterviewGraphContinueRunRequest(BaseModel):
    # 任务标识
    job_type: Literal["START", "CONTINUE"] = Field(..., alias="jobType")
    run_id: int = Field(..., alias="runId")
    session_id: int | None = Field(default=None, alias="sessionId")
    resume_id: int | None = Field(default=None, alias="resumeId")
    scene_code: str | None = Field(default=None, alias="sceneCode")
    job_description: str | None = Field(default=None, alias="jobDescription")

    # CONTINUE 时的答题补充
    answers: list[ClarificationAnswer] = Field(default_factory=list)
