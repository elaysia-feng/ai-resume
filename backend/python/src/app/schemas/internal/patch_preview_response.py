from typing import Any

from pydantic import BaseModel, Field


class PatchPreviewResponse(BaseModel):
    """Java 返回的 patch 预览。"""

    resume_id: int = Field(..., alias="resumeId", description="简历 ID")
    patches: list[dict[str, Any]] = Field(default_factory=list, description="预览后的简历 patch 列表")
