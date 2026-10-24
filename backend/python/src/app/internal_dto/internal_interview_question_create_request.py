from pydantic import BaseModel, Field

from src.app.interview.types import InterviewOption


class InternalInterviewQuestionCreateRequest(BaseModel):
    """Python 发给 Java 的面试题创建请求。"""

    question_text: str = Field(..., alias="questionText", description="题干")
    options: list[InterviewOption] = Field(default_factory=list, description="选项列表，按顺序对应 A/B/C/D")
