from typing import Any

from pydantic import BaseModel, Field


class RunEventPayload(BaseModel):
    """写入 Java 的单条 run event。"""

    event_seq: int = Field(..., alias="eventSeq", description="run 内递增事件序号")
    event_type: str = Field(..., alias="eventType", description="事件类型")
    stage_code: str | None = Field(default=None, alias="stageCode", description="事件所属阶段编码")
    payload: dict[str, Any] = Field(default_factory=dict, description="事件业务载荷")
