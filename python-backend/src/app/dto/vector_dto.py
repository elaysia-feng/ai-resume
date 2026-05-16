from typing import Any

from pydantic import BaseModel, Field


class VectorSearchResult(BaseModel):
    """向量检索结果对象。"""

    text: str = Field(..., description="命中的文本片段")
    score: float = Field(..., description="相似度分数")
    source: str | None = Field(default=None, description="来源标识")
    metadata: dict[str, Any] = Field(default_factory=dict, description="命中片段的 payload 元数据")
