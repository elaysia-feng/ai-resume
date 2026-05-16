from pydantic import BaseModel, Field


class InterviewOptionResponse(BaseModel):
    key: str = Field(..., alias="key")
    text: str = Field(..., alias="text")


class InternalInterviewRoundDetailResponse(BaseModel):
    round_id: int = Field(..., alias="roundId")
    run_id: int = Field(..., alias="runId")
    round_no: int = Field(..., alias="roundNo")
    question_text: str = Field(..., alias="questionText")
    options: list[InterviewOptionResponse] = Field(default_factory=list)
    user_answer: str | None = Field(default=None, alias="userAnswer")
    status: str | None = Field(default=None)
    analysis_json: str | None = Field(default=None, alias="analysisJson")
