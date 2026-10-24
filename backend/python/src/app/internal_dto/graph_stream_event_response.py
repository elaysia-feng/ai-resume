from typing import Any

from pydantic import BaseModel, Field


class GraphStreamEventResponse(BaseModel):
    """Python 内部流事件响应结构。"""

    event_type: str = Field(..., alias="eventType", description="流事件类型")
    data: dict[str, Any] = Field(default_factory=dict, description="流事件业务数据")
