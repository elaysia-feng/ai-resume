import asyncio
import json
import sqlite3
from typing import Literal

from pydantic import BaseModel, Field, ValidationError

from src.app.agent.constants import AgentStatus, InterviewStage
from src.app.config import get_settings
from src.app.internal_dto.interview_graph_run_request import InterviewGraphRunRequest
from src.app.internal_dto.run_status_update_request import RunStatusUpdateRequest
from src.app.interview.graph import InterviewGraphService
from src.app.service.java_gateway_service import java_gateway_service
from src.app.stream import agent_stream

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
    """启动面试模拟的任务队列消费循环。"""
    settings = get_settings()
    # 定义改worker的最大 run 的数量
    max_runs = max(1, settings.python_agent_max_concurrent_runs)
    limiter_run = asyncio.Semaphore(max_runs)
    client = agent_stream.get_client()
    stream = settings.agent_interview_run_stream
    group = settings.agent_interview_worker_group
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

        for message_id, raw, _retry_count in messages:
            await handle_message(client, stream, group, message_id, raw, limiter_run)


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

    except Exception:
        pass

async def handle_message(client, stream, group, message_id, raw, limiter_run: asyncio.Semaphore) -> None:
    """处理单条面试模拟消息"""
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
    async with limiter_run:
        try:
            claimed = await java_gateway_service.claim_run(job.run_id)
            if not claimed:
                return
            await execute_job(job)
        except Exception as e:
            await mark_failed(job.run_id, e)
        finally:
            agent_stream.ack(client, message_id)

if __name__ == "__main__":
    asyncio.run(interview_graph_run_worker())
