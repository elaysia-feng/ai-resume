"""RabbitMQ Agent run worker。

启动方式：
E:\\develop\\Anaconda\\envs\\codex-python\\Scripts\\python.exe -m src.app.worker.agent_run_worker
"""

import asyncio
import json
from typing import Literal

from pydantic import BaseModel, Field, ValidationError

from src.app.agent.constants import AgentStatus
from src.app.agent.graph import agent_graph_service
from src.app.config.settings import get_settings
from src.app.internal_dto.clarification_answer import ClarificationAnswer
from src.app.internal_dto.graph_continue_request import GraphContinueRequest
from src.app.internal_dto.graph_run_stream_request import GraphRunStreamRequest
from src.app.internal_dto.run_status_update_request import RunStatusUpdateRequest
from src.app.service.java_gateway_service import java_gateway_service


class AgentRunJobMessage(BaseModel):
    """RabbitMQ 中的 Agent run 任务消息。

    这个模型要和 Java 的 AgentRunJobMessage 保持一致：
    1. alias 对应 Java JSON 字段名。
    2. START 用于新 run，CONTINUE 用于用户补充信息后的恢复。
    3. 简历详情不放在 MQ 消息里，避免消息体过大；执行时再通过 bootstrap 拉 Java。
    """

    # jobType 是 worker 的分支入口，格式不对的消息会被丢进 DLQ。
    job_type: Literal["START", "CONTINUE"] = Field(..., alias="jobType")
    # runId 同时也是 LangGraph thread_id，用来关联 Redis checkpoint。
    run_id: int = Field(..., alias="runId")
    # START 需要下面这些上下文 ID；CONTINUE 时也保留，方便日志和兜底。
    session_id: int | None = Field(default=None, alias="sessionId")
    resume_id: int | None = Field(default=None, alias="resumeId")
    scene_code: str | None = Field(default=None, alias="sceneCode")
    target_section_id: int | None = Field(default=None, alias="targetSectionId")
    # 用户输入和 JD 是模型判断任务目标的最小文本上下文。
    user_input: str | None = Field(default=None, alias="userInput")
    job_description: str | None = Field(default=None, alias="jobDescription")
    # CONTINUE 时保存 clarifier 的回答；START 时通常为空列表。
    answers: list[ClarificationAnswer] = Field(default_factory=list)


async def run_worker() -> None:
    """启动 RabbitMQ 消费循环。

    并发控制：
    1. RabbitMQ prefetch 控制单 worker 同时拿多少条未 ack 消息。
    2. 本地 semaphore 再兜一层，避免回调并发超过配置。
    3. 每个消息对应一个 LangGraph run，执行结束或挂起后才 ack。
    """
    try:
        import aio_pika
    except ImportError as exc:
        raise RuntimeError("缺少 aio-pika，请先安装 Python 依赖") from exc

    settings = get_settings()
    max_runs = max(1, settings.python_agent_max_concurrent_runs)
    semaphore = asyncio.Semaphore(max_runs)
    connection = await aio_pika.connect_robust(settings.rabbitmq_url)

    async with connection:
        channel = await connection.channel()
        # prefetch 是 RabbitMQ 层面的背压：
        # 一个 worker 最多持有 max_runs 条未 ack 消息，超过的仍留在队列里等待。
        await channel.set_qos(prefetch_count=max_runs)
        # worker 也声明一遍 exchange / queue，避免 Python 比 Java 先启动时找不到队列。
        exchange = await channel.declare_exchange(
            settings.agent_run_exchange,
            aio_pika.ExchangeType.DIRECT,
            durable=True,
        )
        dead_letter_exchange = await channel.declare_exchange(
            settings.agent_run_dead_letter_exchange,
            aio_pika.ExchangeType.DIRECT,
            durable=True,
        )
        # 主队列配置 DLX：worker reject(requeue=False) 的坏消息不会反复重试。
        queue = await channel.declare_queue(
            settings.agent_run_queue,
            durable=True,
            arguments={
                "x-dead-letter-exchange": settings.agent_run_dead_letter_exchange,
                "x-dead-letter-routing-key": settings.agent_run_dead_routing_key,
                "x-queue-mode": "lazy",
            },
        )
        dead_letter_queue = await channel.declare_queue(settings.agent_run_dead_letter_queue, durable=True)
        await queue.bind(exchange, routing_key=settings.agent_run_start_routing_key)
        await queue.bind(exchange, routing_key=settings.agent_run_continue_routing_key)
        await dead_letter_queue.bind(dead_letter_exchange, routing_key=settings.agent_run_dead_routing_key)
        # aio-pika 会为每条消息回调 handle_message；真正并发数由 prefetch + semaphore 共同限制。
        await queue.consume(lambda message: handle_message(message, semaphore))
        # worker 是常驻进程，这个 Future 用来保持事件循环不退出。
        await asyncio.Future()


async def handle_message(message, semaphore: asyncio.Semaphore) -> None:
    """处理单条 RabbitMQ 消息。"""
    try:
        payload = json.loads(message.body.decode("utf-8"))
        job = AgentRunJobMessage.model_validate(payload)
    except (UnicodeDecodeError, json.JSONDecodeError, ValidationError):
        # 格式错误不是业务失败，直接 reject，让 RabbitMQ 按 DLX 配置进入死信队列。
        await message.reject(requeue=False)
        return

    async with semaphore:
        try:
            await execute_job(job)
        except Exception as exc:
            # 业务执行失败也 ack：状态已经写回 FAILED，不让同一条 run 无限重试。
            # 后续如果要自动重试，应单独设计 retry_count 和重试队列。
            await mark_failed(job.run_id, exc)
        finally:
            #  acknowledge（确认）, 就是告诉mq我这个消息已经处理完了
            await message.ack()


async def execute_job(job: AgentRunJobMessage) -> None:
    """执行 START 或 CONTINUE 任务，并 drain 掉 SSE 字符串。"""
    if job.job_type == "START":
        # stream_run 仍然产出 SSE 字符串，生产路径里 Java 不再消费这些字符串。
        # 这里 drain 掉是为了复用原来的 graph 执行入口和事件落库逻辑。
        request = GraphRunStreamRequest.model_validate(job.model_dump(by_alias=True))
        async for _ in agent_graph_service.stream_run(request):
            pass
        return

    # CONTINUE 会把 answers 交给 LangGraph Command(resume=...)，从 checkpoint 恢复执行。
    request = GraphContinueRequest.model_validate(job.model_dump(by_alias=True))
    async for _ in agent_graph_service.continue_run(request):
        pass


async def mark_failed(run_id: int, exc: Exception) -> None:
    """worker 捕获异常后尽量把 Java run 标记为 FAILED。"""
    try:
        # Java 状态表是前端看到的权威状态；失败不能只停留在 worker 日志里。
        await java_gateway_service.update_run_status(
            run_id,
            RunStatusUpdateRequest(status=AgentStatus.FAILED, errorMessage=str(exc)),
        )
    except Exception:
        pass


if __name__ == "__main__":
    asyncio.run(run_worker())
