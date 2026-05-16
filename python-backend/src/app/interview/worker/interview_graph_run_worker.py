import asyncio
import json
from typing import Literal

from pydantic import BaseModel, Field, ValidationError

from app.agent.constants import AgentStatus, InterviewStage
from app.config import get_settings
from app.internal_dto.interview_graph_run_request import InterviewGraphRunRequest
from app.internal_dto.run_status_update_request import RunStatusUpdateRequest
from app.interview.graph import InterviewGraphService
from app.service.java_gateway_service import java_gateway_service

class InterviewRunJobMessage(BaseModel):
    """面试模拟 MQ 消息，和 Java InterviewAgentRunJobMessage 对齐。"""

    job_type: Literal["START", "CONTINUE"] = Field(..., alias="jobType")
    run_id: int = Field(..., alias="runId")
    session_id: int | None = Field(default=None, alias="sessionId")
    resume_id: int | None = Field(default=None, alias="resumeId")
    scene_code: str | None = Field(default=None, alias="sceneCode")
    job_description: str | None = Field(default=None, alias="jobDescription")


# 得到定义的 run和 continue函数
interview_graph_service = InterviewGraphService()


async def interview_graph_run_worker() -> None:
    """启动面试模拟RabbitMq消费循环"""
    try:
        import aio_pika
    except ImportError as exc:
        raise RuntimeError("缺少 aio_pika, 请安装依赖") from exc

    settings = get_settings()
    # 定义改worker的最大 run 的数量
    max_runs = settings.python_agent_max_concurrent_runs if settings.python_agent_max_concurrent_runs is not None else 1
    limiter_run = asyncio.Semaphore(max_runs)
    connection = await aio_pika.connect_robust(settings.rabbitmq_url)

    async with connection:
        # 建立通道
        channel = await connection.channel()
        # 设置消费者允许 mq 一次发送给消费者的消息数量 (如果一次太多的话, 就会导致消费者端的消息堆积太多, 会占用大量内存)
        await channel.set_qos(prefetch_count=max_runs)

        # 设置交换机
        exchange = await channel.declare_exchange(
            settings.agent_run_exchange,
            # 设置交换机的类型
            aio_pika.ExchangeType.DIRECT,
            # 设置持久化
            durable=True,
        )

        # 设置死信交换机
        dead_letter_exchange = await channel.declare_exchange(
            settings.agent_run_dead_letter_exchange,
            aio_pika.ExchangeType.DIRECT,
            durable=True,
        )
        # TODO 跟java端的队列对上
        # 声名队列
        queue = await channel.declare_queue(
            "agent.run.queue",
            durable=True,
            arguments={'x-dead-letter-queue': dead_letter_exchange, "x-dead-letter-routing-key": settings.agent_run_dead_letter_routing_key},
        )

        dead_letter_queue = await channel.declare_queue(
            "agent.interview.run.dlq",
            durable=True,
        )

        await queue.bind(exchange, routing_key=settings.agent_run_routing_key)
        await queue.bind(exchange, routing_key=settings.agent_run_continuation_routing_key)
        await dead_letter_queue.bind(dead_letter_exchange, routing_key=settings.agent_run_dead_letter_routing_key)

        await queue.consume(lambda message: handle_message(message, limiter_run))

        # 让当前协程无限期挂起
        await asyncio.Future()

async def sync_run_status(run_id: int, status: str, result: dict) -> None:
    """把更新的状态回写给java"""
    if status == "WAITING_ANSWER":
        await java_gateway_service.update_run_status(
            run_id=run_id,
            reqeust=RunStatusUpdateRequest(
                status=AgentStatus.WAITING_USER,
                current_stage=InterviewStage.WAITING_ANSWER,
                clarification_payload=result,
            ),
        )
        return

    if status == "IGNORED":
        return
    # 如果不是WAITING_ANSWER这个状态的话, 就还剩
    # 传递答案成功这个状态(但是没用, 如果continue_run里面去用mq通知 java就改一个 status字段成"POST_SUCCESS"的话, 但是我的python的graph已经开始下一个节点的执行了, 前端根本没时间渲染出来, 所以这里就不更新状态了)

    # 还有个情况就是 出到最后一道题了, 调用了 continue_run的时候就直接 把整个图流程执行完了, 这样的话就完成了 Summary节点了, 就更新状态为 Summary就行了
    await java_gateway_service.update_run_status(
        run_id=run_id,
        reqeust=RunStatusUpdateRequest(
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
            run_id=run_id,
            rqeust=RunStatusUpdateRequest(
                status="FAILED",
                error_message=str(exc),
            )
        )

    except Exception:
        pass

async def handle_message(message, limiter_run: asyncio.Semaphore) -> None:
    """处理单条面试模拟消息"""
    try:
        payload = json.loads(message.body.decode("utf-8"))
        # 校验传递的message格式对不对
        job = InterviewRunJobMessage.model_validate(message)
    except (UnicodeDecodeError, json.JSONDecodeError, ValidationError):
        # 不对的话直接拒绝
        await message.reject(requeue=False)
        return
    # 如果场景不是面试的话直接拒绝
    if job.scene_code != "INTERVIEW":
        await message.ack()
        return
    # 如果是的话就直接执行 把message里面的任务执行
    async with limiter_run as limiter:
        try:
            await execute_job(job)
        except Exception as e:
            await mark_failed(job.run_id, e)
        finally:
            await message.ack()

if __name__ == "__main__":
    asyncio.run(interview_graph_run_worker())