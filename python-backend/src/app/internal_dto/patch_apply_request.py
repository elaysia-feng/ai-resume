from typing import Any

from pydantic import BaseModel, Field


class PatchApplyRequest(BaseModel):
    """请求 Java 应用 patch。"""

    run_id: int = Field(..., alias="runId", description="Java 侧 ai_agent_run 主键")
    patches: list[dict[str, Any]] = Field(default_factory=list, description="待应用的简历 patch 列表")
