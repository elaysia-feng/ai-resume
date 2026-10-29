from app.graphs.resume_agent.events import AgentEvent
from app.schemas.internal.run_status_update_request import RunStatusUpdateRequest
from app.services.java_gateway_service import java_gateway_service


async def persist_run_events_tool(run_id: int, events: list[AgentEvent]) -> None:
    """持久化 run event。"""
    await java_gateway_service.push_run_events(run_id, events)


async def update_run_status_tool(run_id: int, status: str, payload: dict) -> None:
    """更新 Java run 状态。"""
    await java_gateway_service.update_run_status(
        run_id,
        RunStatusUpdateRequest(status=status, **payload),
    )
