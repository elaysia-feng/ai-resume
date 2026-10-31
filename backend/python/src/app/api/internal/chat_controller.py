"""由 Java 调用的内部工作区聊天接口。"""

import hmac
from collections.abc import AsyncIterator

from fastapi import APIRouter, Header, HTTPException
from fastapi.responses import StreamingResponse

from app.core.config import get_settings
from app.schemas.internal.workspace_chat_request import WorkspaceChatRequest
from app.services.workspace_chat_service import workspace_chat_service

router = APIRouter(prefix="/internal/chat", tags=["internal-chat"])


def _verify_internal_token(token: str) -> None:
    expected = get_settings().internal_agent_service_token
    if not expected or not token or not hmac.compare_digest(token, expected):
        raise HTTPException(status_code=401, detail="未授权的内部服务请求")


@router.post("/stream")
async def stream_chat(
    request: WorkspaceChatRequest,
    x_internal_service_token: str = Header(default=""),
) -> StreamingResponse:
    """校验 Java 内部令牌后，代理 ChatModel 与工作区工具的 SSE 事件。

    Args:
        request: Java 校验过会话归属的聊天上下文。
        x_internal_service_token: Java 到 Python 的内部服务令牌。

    Returns:
        转发模型和工作区工具事件的 SSE 响应。
    """
    _verify_internal_token(x_internal_service_token)

    async def events() -> AsyncIterator[str]:
        async for event in workspace_chat_service.stream(request):
            yield event

    return StreamingResponse(events(), media_type="text/event-stream")
