import json
from typing import Any

from pydantic import BaseModel, Field


class AgentEvent(BaseModel):
    """Agent 事件对象，用于 SSE 输出和 Java 落库。"""

    event_seq: int = Field(..., alias="eventSeq", description="当前 run 内递增的事件序号，用于前端按顺序展示事件")
    event_type: str = Field(..., alias="eventType", description="事件类型，例如 run.started、stage.changed、approval.required")
    run_id: int = Field(..., alias="runId", description="Java 侧 ai_agent_run 主键，用于定位本次 Agent 执行")
    session_id: int | None = Field(default=None, alias="sessionId", description="AI 会话 ID，用于把事件归属到具体会话")
    stage_code: str | None = Field(default=None, alias="stageCode", description="当前事件所属阶段编码，例如 BOOTSTRAP、REWRITER")
    message: str | None = Field(default=None, description="面向前端展示的简短事件说明")
    payload: dict[str, Any] = Field(default_factory=dict, description="事件附加业务数据，不同 eventType 结构不同")


class AgentEventBuilder:
    """AgentEvent 构造器。"""

    def build(
        self,
        *,
        event_seq: int,
        event_type: str,
        run_id: int,
        session_id: int | None = None,
        stage_code: str | None = None,
        message: str | None = None,
        payload: dict[str, Any] | None = None,
    ) -> AgentEvent:
        """构造标准事件。"""
        return AgentEvent(
            eventSeq=event_seq,
            eventType=event_type,
            runId=run_id,
            sessionId=session_id,
            stageCode=stage_code,
            message=message,
            payload=payload or {},
        )


def format_sse_event(event: AgentEvent) -> str:
    """将 AgentEvent 格式化成 SSE 字符串。"""
    data = event.model_dump(by_alias=True)
    return (
        f"id: {event.event_seq}\n"
        f"event: {event.event_type}\n"
        f"data: {json.dumps(data, ensure_ascii=False)}\n\n"
    )


agent_event_builder = AgentEventBuilder()
