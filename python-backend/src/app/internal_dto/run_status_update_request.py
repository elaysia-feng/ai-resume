from typing import Any

from pydantic import BaseModel, Field


class RunStatusUpdateRequest(BaseModel):
    """Python 回写 Java 的 run 状态请求。"""

    status: str = Field(..., description="要回写的 run 状态")
    current_stage: str | None = Field(default=None, alias="currentStage", description="当前执行阶段")
    result_summary: str | None = Field(default=None, alias="resultSummary", description="运行结果摘要")
    clarification_payload: dict[str, Any] | None = Field(default=None, alias="clarificationPayload", description="需要用户补充的信息")
    approval_payload: dict[str, Any] | None = Field(default=None, alias="approvalPayload", description="待用户确认的审批包")
    error_code: str | None = Field(default=None, alias="errorCode", description="失败错误码")
    error_message: str | None = Field(default=None, alias="errorMessage", description="失败错误信息")
