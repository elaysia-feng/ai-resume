"""SQLite 任务队列的 Agent run worker。

启动方式：
python -m src.app.worker.agent_run_worker
"""

import asyncio
import json
import logging
import sqlite3
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
from src.app.stream import agent_stream

logger = logging.getLogger("uvicorn.error")


class AgentRunJobMessage(BaseModel):
    """任务队列中的 Agent run 任务消息。

    这个模型要和 Java 的 AgentRunJobMessage 保持一致：
    1. alias 对应 Java JSON 字段名。
    2. START 用于新 run，CONTINUE 用于用户补充信息后的恢复。
    3. 简历详情不放在消息里，避免消息体过大；执行时再通过 bootstrap 拉 Java。
    """

    # jobType 是 worker 的分支入口，格式不对的消息会被转投到死信流。
    job_type: Literal["START", "CONTINUE"] = Field(..., alias="jobType")
    # runId 同时也是 LangGraph thread_id，用来关联 checkpoint。
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
    """启动任务队列消费循环。

    并发控制：
    1. 每次最多认领 max_runs 条未处理消息。
    2. 本地 semaphore 再兜一层，避免并发处理数超过配置。
    3. 每个消息对应一个 LangGraph run，执行结束或挂起后才 ack。
    """
    settings = get_settings()
    max_runs = max(1, settings.python_agent_max_concurrent_runs)
    poll_interval = max(0.05, settings.agent_task_poll_interval_seconds)
    semaphore = asyncio.Semaphore(max_runs)
    client = agent_stream.get_client()
    stream = settings.agent_resume_run_stream
    group = settings.agent_resume_worker_group
    consumer = f"{group}-1"

    # 先登记消费组，避免 Java 先投递时消息无处可挂。
    agent_stream.ensure_group(client, stream, group)

    while True:
        try:
            messages = agent_stream.read_batch(client, stream, group, consumer, max_runs)
        except sqlite3.OperationalError:
            # 数据库被占用或不可用，等一会儿再试，不要让整个 worker 退出。
            await asyncio.sleep(2)
            continue

        if not messages:
            # 队列为空时必须让出控制权。
            # 桌面端把 worker 挂在 FastAPI 的 lifespan 里（见 src/app/main.py），
            # 这个循环如果一次都不 await，就会独占 event loop，
            # uvicorn 的 startup 永远完不成，HTTP 接口根本起不来。
            await asyncio.sleep(poll_interval)
            continue

        for message_id, raw, _retry_count in messages:
            await handle_message(client, stream, group, message_id, raw, semaphore)


async def handle_message(
    client,
    stream: str,
    group: str,
    message_id: str,
    raw: str,
    semaphore: asyncio.Semaphore,
) -> None:
    """处理单条任务消息。"""
    settings = get_settings()
    try:
        job = AgentRunJobMessage.model_validate(json.loads(raw))
    except (json.JSONDecodeError, TypeError, ValidationError):
        # 格式错误不是业务失败，直接转投死信流。
        agent_stream.to_dead_letter(
            client,
            settings.agent_run_dead_letter_stream,
            settings.agent_dlq_consumer_group,
            message_id,
            raw,
            "invalid_payload",
        )
        agent_stream.ack(client, message_id)
        return

    async with semaphore:
        try:
            claimed = await java_gateway_service.claim_run(job.run_id)
            if not claimed:
                return
            await execute_job(job)
        except Exception as exc:
            # 业务执行失败也 ack：状态已经写回 FAILED，不让同一条 run 无限重试。
            # 后续如果要自动重试，应单独设计 retry_count 和重试队列。
            await mark_failed(job.run_id, exc)
        finally:
            # 确认消息已处理完成，对应任务从队列里移出。
            agent_stream.ack(client, message_id)


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


async def run_worker_forever() -> None:
    """worker 的守护入口：异常后等一下再重试，不让进程静默死掉。

    桌面端把 worker 挂在 FastAPI 的 lifespan 里（见 src/app/main.py），
    一次偶发异常如果让 task 直接结束，任务队列就再也没人消费了，
    而且不会有任何报错——所以这里必须自己兜住。
    """
    while True:
        try:
            await run_worker()
            return
        except asyncio.CancelledError:
            raise
        except Exception as exc:
            logger.exception("Agent worker 异常退出，5 秒后重试: %s", exc)
            await asyncio.sleep(5)


if __name__ == "__main__":
    asyncio.run(run_worker_forever())
