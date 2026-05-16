from fastapi import FastAPI

from .config.settings import get_settings
from .router import api_router

settings = get_settings()

app = FastAPI(title=settings.app_name)
app.include_router(api_router)


@app.get("/")
def root() -> dict[str, str]:
    """返回服务名称，方便本地确认服务已启动。"""
    return {"message": settings.app_name}
