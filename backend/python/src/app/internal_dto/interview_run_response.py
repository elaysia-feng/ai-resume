from pydantic import BaseModel


class InterviewStartResponse(BaseModel):
    """start_run 返回：图暂停在 interrupt，等待用户答题。"""

    status: str  # WAITING_ANSWER
    round_id: int
    round_no: int
    question: dict  # 题目数据


class InterviewContinueResponse(BaseModel):
    """continue_run 返回：图恢复后可能又暂停，或面试结束。"""

    status: str  # WAITING_ANSWER / SUCCESS
    round_id: int | None = None
    round_no: int | None = None
    question: dict | None = None  # 下一题，status=WAITING_ANSWER 时有值
    summary: dict | None = None   # 面试总结，status=SUCCESS 时有值

