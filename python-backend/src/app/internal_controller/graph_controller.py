from fastapi import APIRouter
from fastapi.responses import StreamingResponse

from src.app.agent.graph import agent_graph_service
from src.app.internal_dto.graph_continue_request import GraphContinueRequest
from src.app.internal_dto.graph_run_stream_request import GraphRunStreamRequest

router = APIRouter(prefix="/internal/graph", tags=["internal-graph"])


@router.post("/runs/stream")
async def start_run_stream(request: GraphRunStreamRequest) -> StreamingResponse:
    """启动 Agent run，返回 SSE 事件流。

    Controller 只负责 HTTP 入参和响应类型适配：
    - 不直接写业务流程
    - 不直接调用 LLM
    - 不直接访问 Java 内部 API

    真正的 Agent 编排由 AgentGraphService 和 LangGraph 节点完成。
    """
    return StreamingResponse(
        agent_graph_service.stream_run(request),
        media_type="text/event-stream",
    )


@router.post("/runs/{run_id}/continue/stream")
async def continue_run_stream(run_id: int, request: GraphContinueRequest) -> StreamingResponse:
    """用户补充信息后继续 Agent run，返回 SSE 事件流。

    run_id 放在 path 中，request body 只承载用户回答。
    这里把 path 里的 run_id 写回 DTO，保持后续 service 只接收一个请求对象。
    """
    request.run_id = run_id
    return StreamingResponse(
        agent_graph_service.continue_run(request),
        media_type="text/event-stream",
    )


@router.post("/runs/{run_id}/cancel")
async def cancel_run(run_id: int) -> dict[str, int]:
    """取消 Agent run。

    Controller 不关心 Redis checkpoint 和 Java 状态同步细节，只委托给 AgentGraphService。
    """
    await agent_graph_service.cancel_run(run_id)
    return {"runId": run_id}
