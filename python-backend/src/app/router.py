from fastapi import APIRouter

from .controller.dev_resume_controller import router as dev_resume_router
from .controller.health_controller import router as health_router
from .internal_controller.graph_controller import router as graph_router

# 统一汇总所有 controller 的路由，避免在 main.py 里逐个注册。
# 分层约定：
# - controller / internal_controller：只做 HTTP 协议适配。
# - agent/nodes：承载 Agent 业务步骤和 state 流转。
# - service：封装外部系统、模型、校验、patch 等可复用能力。
api_router = APIRouter()
api_router.include_router(health_router)
api_router.include_router(dev_resume_router)
api_router.include_router(graph_router)
