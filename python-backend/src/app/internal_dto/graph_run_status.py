from typing import Any

from pydantic import BaseModel, Field


class GraphRunStatus(BaseModel):
    """Agent run 状态响应。"""

    run_id: int = Field(..., alias="runId", description="Java 侧 ai_agent_run 主键")
    status: str = Field(..., description="run 当前状态")
    current_stage: str | None = Field(default=None, alias="currentStage", description="当前执行阶段")
    payload: dict[str, Any] = Field(default_factory=dict, description="状态附加数据")
