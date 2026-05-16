from fastapi import APIRouter

from ..dto.common_dto import HealthResponse

# 健康检查接口，方便联调时确认 Python 服务可达。
router = APIRouter(tags=["health"])


@router.get("/api/health", response_model=HealthResponse)
def health() -> HealthResponse:
    """返回固定健康状态。"""
    return HealthResponse(status="ok")
