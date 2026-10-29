from app.schemas.internal.bootstrap_request import BootstrapRequest
from app.schemas.internal.bootstrap_response import BootstrapResponse
from app.services.java_gateway_service import java_gateway_service


async def load_bootstrap_context_tool(request: BootstrapRequest) -> BootstrapResponse:
    """加载简历、会话、schema、版本等上下文。"""
    return await java_gateway_service.load_bootstrap_context(request)
