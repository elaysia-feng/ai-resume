from pydantic import BaseModel, Field


class InternalInterviewQuestionCreateResponse(BaseModel):
    """Java 返回给 Python 的面试题创建结果。"""

    round_id: int = Field(..., alias="roundId", description="题目轮次记录ID")
    round_no: int = Field(..., alias="roundNo", description="第几轮问题，从1开始")
