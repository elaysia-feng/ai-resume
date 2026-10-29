from fastapi import APIRouter

from app.api.dev_resume_controller import router as dev_resume_router
from app.api.health_controller import router as health_router
from app.api.internal.graph_controller import router as graph_router

# 统一汇总所有 API 路由，避免在 main.py 里逐个注册。
# 分层约定：
# - api：只做 HTTP 协议适配。
# - graphs：承载 Agent 业务步骤和 state 流转。
# - services：封装外部系统、模型、校验、patch 等应用能力。
api_router = APIRouter()
api_router.include_router(health_router)
api_router.include_router(dev_resume_router)
api_router.include_router(graph_router)
