from pydantic import BaseModel, Field

from app.schemas.internal.run_event_payload import RunEventPayload


class RunEventBatchRequest(BaseModel):
    """批量写入 Java 的 run event 请求。"""

    events: list[RunEventPayload] = Field(default_factory=list, description="待批量写入的 run event 列表")
