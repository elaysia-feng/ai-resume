import asyncio
import json
import logging
import sqlite3
import time
from typing import Literal

from pydantic import BaseModel, Field, ValidationError

from app.core.config import get_settings
from app.graphs.interview.graph import InterviewGraphService
from app.graphs.resume_agent.constants import AgentStatus, InterviewStage
from app.schemas.internal.interview_graph_run_request import InterviewGraphRunRequest
from app.schemas.internal.run_status_update_request import RunStatusUpdateRequest
from app.services.java_gateway_service import java_gateway_service
from app.stream import agent_stream

logger = logging.getLogger("uvicorn.error")
READ_BATCH_SIZE = 4

class InterviewRunJobMessage(BaseModel):
    """面试模拟任务消息，和 Java InterviewAgentRunJobMessage 对齐。"""

    job_type: Literal["START", "CONTINUE"] = Field(..., alias="jobType")
    run_id: int = Field(..., alias="runId")
    session_id: int | None = Field(default=None, alias="sessionId")
    resume_id: int | None = Field(default=None, alias="resumeId")
    scene_code: str | None = Field(default=None, alias="sceneCode")
    job_description: str | None = Field(default=None, alias="jobDescription")


# 得到定义的 run和 continue函数
interview_graph_service = InterviewGraphService()


async def interview_graph_run_worker() -> None:
    """启动面试模拟的任务队列消费循环。

    1. 初始化消费组和任务跟踪集合。
    2. 轮询队列，为每条消息创建独立的后台任务，不等待上一批结束。
    3. worker 退出时取消并等待仍在运行的任务。
    """
    # 1. 初始化配置、队列客户端和消费组。
    settings = get_settings()
    poll_interval = max(0.05, settings.agent_task_poll_interval_seconds)
    active_tasks: set[asyncio.Task[None]] = set()
    client = agent_stream.get_client()
    stream = settings.agent_interview_run_stream
    group = settings.agent_interview_worker_group
    consumer = f"{group}-1"

    # 先登记消费组，避免 Java 先投递时消息无处可挂。
    agent_stream.ensure_group(client, stream, group)

    # 陈旧认领回收：与简历 worker 相同，防止崩溃残留消息永远卡在 CLAIMED。
    last_reclaim_at = 0.0

    # 2. 持续拉取消息并为每个面试 run 建立独立任务。
    try:
        while True:
            if time.monotonic() - last_reclaim_at >= 60:
                try:
                    agent_stream.reclaim_stale(
                        client,
                        stream,
                        group,
                        timeout_seconds=settings.agent_task_claim_timeout_seconds,
                    )
                except sqlite3.OperationalError:
                    logger.warning("面试队列回收陈旧消息失败，等待下一轮重试")
                last_reclaim_at = time.monotonic()

            try:
                messages = agent_stream.read_batch(client, stream, group, consumer, READ_BATCH_SIZE)
            except sqlite3.OperationalError:
                # 数据库被占用或不可用，等一会儿再试，不要让整个 worker 退出。
                await asyncio.sleep(2)
                continue

            if not messages:
                # 队列为空时让出 event loop：忙循环会饿死同进程的 HTTP 服务。
                await asyncio.sleep(poll_interval)
                continue

            for message_id, raw, _retry_count in messages:
                task = asyncio.create_task(handle_message(client, stream, group, message_id, raw))
                active_tasks.add(task)
                task.add_done_callback(lambda completed: _finish_worker_task(active_tasks, completed))
            await asyncio.sleep(0)
    finally:
        # 3. worker 退出时取消并回收本进程仍在运行的任务。
        pending_tasks = tuple(active_tasks)
        for task in pending_tasks:
            task.cancel()
        if pending_tasks:
            await asyncio.gather(*pending_tasks, return_exceptions=True)


def _finish_worker_task(active_tasks: set[asyncio.Task[None]], task: asyncio.Task[None]) -> None:
    active_tasks.discard(task)
    try:
        task.result()
    except asyncio.CancelledError:
        return
    except Exception:
        logger.exception("面试 Agent 后台任务异常退出")


async def sync_run_status(run_id: int, status: str, result: dict) -> None:
    """把更新的状态回写给java"""
    if status == "WAITING_ANSWER":
        await java_gateway_service.update_run_status(
            run_id,
            RunStatusUpdateRequest(
                status=AgentStatus.WAITING_USER,
                current_stage=InterviewStage.WAITING_ANSWER,
                clarification_payload=result,
            ),
        )
        return

    if status == "IGNORED":
        return
    # 如果不是WAITING_ANSWER这个状态的话, 就还剩
    # 传递答案成功这个状态(但是没用, 如果continue_run里面去用队列通知 java就改一个 status字段成"POST_SUCCESS"的话, 但是我的python的graph已经开始下一个节点的执行了, 前端根本没时间渲染出来, 所以这里就不更新状态了)

    # 还有个情况就是 出到最后一道题了, 调用了 continue_run的时候就直接 把整个图流程执行完了, 这样的话就完成了 Summary节点了, 就更新状态为 Summary就行了
    await java_gateway_service.update_run_status(
        run_id,
        RunStatusUpdateRequest(
            status=status,
            current_stage="SUMMARY",
            result_summary=json.dumps(result.get("summary", {}), ensure_ascii=False),
        )
    )




async def execute_job(job) -> None:
    """执行START 或者 CONTINUE 任务"""
    request = InterviewGraphRunRequest.model_validate(job.model_dump(by_alias=True))

    if job.job_type == "START":
        response = await interview_graph_service.start_run(request)
    else :
        response = await interview_graph_service.continue_run(request)

    #
    await sync_run_status(job.run_id, response.status, response.model_dump(exclude_none=True))

async def mark_failed(run_id: int, exc: Exception) -> None:
    """worker 捕获异常后经量把 java_run标记为failed"""

    try:
        await java_gateway_service.update_run_status(
            run_id,
            RunStatusUpdateRequest(
                status=AgentStatus.FAILED,
                error_message=str(exc),
            )
        )

    except Exception as report_exc:
        # 回写失败时至少留下日志，否则 run 会永远停在 RUNNING 且无人知晓。
        logger.warning("回写面试 run %s 失败状态出错: %s", run_id, report_exc)

async def handle_message(client, stream, group, message_id, raw) -> None:
    """处理单条面试模拟消息。

    Args:
        client: 任务队列 SQLite 客户端。
        stream: 当前任务流名称。
        group: 当前消费组名称。
        message_id: 队列消息编号。
        raw: 队列中的原始 JSON 消息。
    """
    settings = get_settings()
    try:
        # 校验传递的message格式对不对
        job = InterviewRunJobMessage.model_validate(json.loads(raw))
    except (json.JSONDecodeError, TypeError, ValidationError):
        # 不对的话直接转投死信流
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
    # 如果场景不是面试的话直接确认掉
    if job.scene_code != "INTERVIEW":
        agent_stream.ack(client, message_id)
        return
    # 如果是的话就直接执行 把message里面的任务执行
    # 1. 向 Java 认领 run，再执行面试图流程并同步结果。
    try:
        claimed = await java_gateway_service.claim_run(job.run_id)
        if not claimed:
            return
        await execute_job(job)
    except Exception as e:
        await mark_failed(job.run_id, e)
    finally:
        # 2. 处理结束后确认队列消息。
        agent_stream.ack(client, message_id)

if __name__ == "__main__":
    asyncio.run(interview_graph_run_worker())
