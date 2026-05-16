import json
from typing import Any

import httpx

from app.internal_dto.internal_interview_round_detail_response import InternalInterviewRoundDetailResponse
from app.internal_dto.interview_bootstrap_request import InterviewBootstrapRequest
from app.internal_dto.interview_bootstrap_response import InterviewBootstrapResponse
from app.internal_dto.internal_interview_question_create_request import InternalInterviewQuestionCreateRequest
from app.internal_dto.internal_interview_question_create_response import InternalInterviewQuestionCreateResponse
from src.app.agent.constants import AgentStatus
from src.app.agent.events import AgentEvent
from src.app.config.settings import get_settings
from src.app.internal_dto.bootstrap_request import BootstrapRequest
from src.app.internal_dto.bootstrap_response import BootstrapResponse
from src.app.internal_dto.patch_preview_request import PatchPreviewRequest
from src.app.internal_dto.patch_preview_response import PatchPreviewResponse
from src.app.internal_dto.run_status_update_request import RunStatusUpdateRequest


class JavaGatewayService:
    """Python 调 Java 内部 API 的统一入口。

    这是基础设施服务，不写 Agent 主业务流程。
    它只负责：
    - 统一 Java 内部接口 base_url、header、token
    - 把 Pydantic DTO 转成 JSON
    - 把 Java JSON 响应转回 Python DTO

    简历权限、版本落库、patch 应用仍以 Java 为准，Python 不绕过 Java 直连业务库。
    """

    def __init__(self) -> None:
        self.settings = get_settings()

    def build_headers(self) -> dict[str, str]:
        """构造 Java 内部接口请求头。"""
        headers = {"Content-Type": "application/json"}
        if self.settings.internal_agent_service_token:
            headers["X-Internal-Service-Token"] = self.settings.internal_agent_service_token
        return headers

    def _to_json_body(self, body: Any) -> Any:
        """把请求体转换成 httpx 可发送的 JSON 数据。"""
        if hasattr(body, "model_dump"):
            return body.model_dump(by_alias=True, exclude_none=True)
        return body

    # 发送消息给mq
    # TODO 用的默认交换机, 后面复杂了可以自己自定义交换机
    async def _publish_result(self, message : dict) -> None:
        try:
            import aio_pika
        except ImportError as exc:
            raise RuntimeError("缺少 aio-pika，请先安装 Python 依赖") from exc
        connection = aio_pika.connect_robust(self.settings.rabbitmq_url)

        async with connection:
            channel = await connection.channel()
            queue = await channel.declare_queue(self.settings.agent_run_result_queue, durable=True)
            await channel.default_exchange.publish(
                aio_pika.Message(body=json.dumps(message).encode("utf-8")),
                routing_key=queue.name,
            )


    async def post_json(self, path: str, body: Any | None) -> dict:
        """发送 JSON POST 请求到 Java。"""
        url = f"{self.settings.java_internal_base_url.rstrip('/')}{path}"
        async with httpx.AsyncClient(timeout=self.settings.java_internal_timeout_seconds) as client:
            response = await client.post(url, json=self._to_json_body(body), headers=self.build_headers())
            response.raise_for_status()
            return response.json() if response.content else {}

    async def get_json(self, path: str) -> dict:
        """发送 JSON GET 请求到 Java。"""
        url = f"{self.settings.java_internal_base_url.rstrip('/')}{path}"
        async with httpx.AsyncClient(timeout=self.settings.java_internal_timeout_seconds) as client:
            response = await client.get(url, headers=self.build_headers())
            response.raise_for_status()
            return response.json() if response.content else {}
    # 马上需要的, 不能接入mq
    async def load_bootstrap_context(self, request: BootstrapRequest) -> BootstrapResponse:
        """调用 Java 内部接口加载简历、会话、schema 上下文。"""
        data = await self.post_json("/internal/agent/bootstrap", request)
        return BootstrapResponse.model_validate(data)

    async def load_interview_bootstrap_context(self, request: InterviewBootstrapRequest) -> InterviewBootstrapResponse:
        """调用 Java 内部接口加载面试模拟上下文。"""
        data = await self.post_json("/internal/agent/interviewBootstrap", request)
        return InterviewBootstrapResponse.model_validate(data)

    # 可以异步写入的可以接入mq
    async def push_run_events(self, run_id: int, events: list[AgentEvent]) -> None:
        """批量写入 run event 到 Java。"""
        body = {
            "action": "PUSH_EVENTS",
            "runId": run_id,
            "events": [event.model_dump(by_alias=True, exclude_none=True) for event in events]
        }
        await self._publish_result(body)

    # 可以异步更新的可以接入mq
    async def update_run_status(self, run_id: int, request: RunStatusUpdateRequest) -> None:
        """回写 run 状态到 Java。"""
        body = {
            "action": "UPDATE_STATUS",
            "runId": run_id,
            **request.model_dump(by_alias=True, exclude_none=True),
        }
        await self._publish_result(body)

    async def preview_patch(self, resume_id: int, patches: list[dict]) -> dict:
        """调用 Java patch 预览接口。"""
        request = PatchPreviewRequest(patches=patches)
        return await self.post_json(f"/internal/resumes/{resume_id}/patch-preview", request)

    async def preview_patch_typed(self, resume_id: int, request: PatchPreviewRequest) -> PatchPreviewResponse:
        """调用 Java patch 预览接口并返回类型化响应。"""
        data = await self.post_json(f"/internal/resumes/{resume_id}/patch-preview", request)
        return PatchPreviewResponse.model_validate(data)
    # 异步通知
    async def notify_cancelled(self, run_id: int) -> None:
        """通知 Java 当前 run 已在 Python 侧取消。"""
        await self.update_run_status(run_id, RunStatusUpdateRequest(status=AgentStatus.CANCELLED))


    # 让agent创建问题
    async def create_question_round(
        self,
        run_id: int,
        request: InternalInterviewQuestionCreateRequest,
    ) -> InternalInterviewQuestionCreateResponse:
        """把 Python 生成的题目写回 Java。"""
        data = await self.post_json(f"/internal/agent/interview/runs/{run_id}/rounds", request)
        return InternalInterviewQuestionCreateResponse.model_validate(data)

    # 获取用户回答的问题
    async def get_question_answer(self, round_id: int) -> InternalInterviewRoundDetailResponse:
        """把用户的回答返回给agent"""
        data = await self.get_json(f"/internal/agent/interview/rounds/{round_id}/answers")
        return InternalInterviewRoundDetailResponse.model_validate(data)



java_gateway_service = JavaGatewayService()
