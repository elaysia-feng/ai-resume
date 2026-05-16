from collections.abc import AsyncIterator

from src.app.agent.constants import AgentEventType
from src.app.agent.events import AgentEvent, format_sse_event


class AgentRuntime:
    """Agent 流式运行辅助类。"""

    async def stream_events(self, events: AsyncIterator[AgentEvent]) -> AsyncIterator[str]:
        """把 AgentEvent 流转换为 SSE 字符串流。"""
        async for event in events:
            yield format_sse_event(event)

    async def handle_error(self, run_id: int, exc: Exception) -> AgentEvent:
        """把异常转换为 run.failed 事件。"""
        return AgentEvent(
            eventSeq=1,
            eventType=AgentEventType.RUN_FAILED,
            runId=run_id,
            message=str(exc),
        )


agent_runtime = AgentRuntime()
