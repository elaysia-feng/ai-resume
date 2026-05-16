from typing import Any

from pydantic import BaseModel, Field


class PatchPreviewRequest(BaseModel):
    """请求 Java 预览 patch。"""

    patches: list[dict[str, Any]] = Field(default_factory=list, description="待预览的简历 patch 列表")
