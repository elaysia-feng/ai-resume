from typing import Any

from src.app.agent.constants import AgentEventType
from src.app.agent.events import AgentEvent
from src.app.service.java_gateway_service import java_gateway_service


class AgentEventService:
    """Agent 事件构造和上报服务。

    这里不是业务编排层，只负责把 Agent 运行过程包装成统一事件对象，
    并通过 JavaGatewayService 持久化到 Java。
    """

    def build_event(
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
        """构造标准 AgentEvent。"""
        return AgentEvent(
            eventSeq=event_seq,
            eventType=event_type,
            runId=run_id,
            sessionId=session_id,
            stageCode=stage_code,
            message=message,
            payload=payload or {},
        )

    async def persist_events(self, run_id: int, events: list[AgentEvent]) -> None:
        """持久化事件到 Java。"""
        if events:
            await java_gateway_service.push_run_events(run_id, events)

    def run_started(self, *, event_seq: int, run_id: int, session_id: int | None = None) -> AgentEvent:
        """构造 run.started 事件。"""
        return self.build_event(
            event_seq=event_seq,
            event_type=AgentEventType.RUN_STARTED,
            run_id=run_id,
            session_id=session_id,
            message="Agent run 已启动",
        )

    def stage_changed(
        self,
        *,
        event_seq: int,
        run_id: int,
        stage_code: str,
        message: str,
        session_id: int | None = None,
    ) -> AgentEvent:
        """构造 stage.changed 事件。"""
        return self.build_event(
            event_seq=event_seq,
            event_type=AgentEventType.STAGE_CHANGED,
            run_id=run_id,
            session_id=session_id,
            stage_code=stage_code,
            message=message,
        )

    def tool_started(
        self,
        *,
        event_seq: int,
        run_id: int,
        tool_name: str,
        stage_code: str | None = None,
        session_id: int | None = None,
    ) -> AgentEvent:
        """构造 tool.started 事件。"""
        return self.build_event(
            event_seq=event_seq,
            event_type=AgentEventType.TOOL_STARTED,
            run_id=run_id,
            session_id=session_id,
            stage_code=stage_code,
            message=f"开始调用工具: {tool_name}",
            payload={"toolName": tool_name},
        )

    def tool_finished(
        self,
        *,
        event_seq: int,
        run_id: int,
        tool_name: str,
        stage_code: str | None = None,
        session_id: int | None = None,
        payload: dict[str, Any] | None = None,
    ) -> AgentEvent:
        """构造 tool.finished 事件。"""
        return self.build_event(
            event_seq=event_seq,
            event_type=AgentEventType.TOOL_FINISHED,
            run_id=run_id,
            session_id=session_id,
            stage_code=stage_code,
            message=f"工具调用完成: {tool_name}",
            payload={"toolName": tool_name, **(payload or {})},
        )

    def assistant_delta(
        self,
        *,
        event_seq: int,
        run_id: int,
        message: str,
        session_id: int | None = None,
    ) -> AgentEvent:
        """构造 assistant.delta 事件。"""
        return self.build_event(
            event_seq=event_seq,
            event_type=AgentEventType.ASSISTANT_DELTA,
            run_id=run_id,
            session_id=session_id,
            message=message,
        )

    def clarification_required(
        self,
        *,
        event_seq: int,
        run_id: int,
        payload: dict[str, Any],
        session_id: int | None = None,
    ) -> AgentEvent:
        """构造 clarification.required 事件。"""
        return self.build_event(
            event_seq=event_seq,
            event_type=AgentEventType.CLARIFICATION_REQUIRED,
            run_id=run_id,
            session_id=session_id,
            message="需要补充信息",
            payload=payload,
        )

    def approval_required(
        self,
        *,
        event_seq: int,
        run_id: int,
        payload: dict[str, Any],
        session_id: int | None = None,
    ) -> AgentEvent:
        """构造 approval.required 事件。"""
        return self.build_event(
            event_seq=event_seq,
            event_type=AgentEventType.APPROVAL_REQUIRED,
            run_id=run_id,
            session_id=session_id,
            message="已生成待确认修改建议",
            payload=payload,
        )

    def run_failed(
        self,
        *,
        event_seq: int,
        run_id: int,
        error_message: str,
        session_id: int | None = None,
    ) -> AgentEvent:
        """构造 run.failed 事件。"""
        return self.build_event(
            event_seq=event_seq,
            event_type=AgentEventType.RUN_FAILED,
            run_id=run_id,
            session_id=session_id,
            message=error_message,
        )


agent_event_service = AgentEventService()
