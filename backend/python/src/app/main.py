import asyncio
from contextlib import asynccontextmanager, suppress

from fastapi import FastAPI

from .config.settings import get_settings
from .router import api_router
from .worker.agent_run_worker import run_worker_forever


@asynccontextmanager
async def lifespan(app: FastAPI):
    """把 Agent 任务队列 worker 挂到 FastAPI 进程里一起跑。

    桌面端只拉起一个 Python 进程就够了：
    1. 之前 worker 是独立入口（python -m src.app.worker.agent_run_worker），
       桌面端就得管两个 Python 进程。
    2. 合并后打包产物也只有一份，PyInstaller 只需要打一个 exe。

    worker 挂掉不影响 HTTP 接口：它在独立 task 里跑，异常自己吞掉重试。
    """
    worker_task = asyncio.create_task(run_worker_forever())
    try:
        yield
    finally:
        worker_task.cancel()
        with suppress(asyncio.CancelledError):
            await worker_task


settings = get_settings()

app = FastAPI(title=settings.app_name, lifespan=lifespan)
app.include_router(api_router)


@app.get("/")
def root() -> dict[str, str]:
    """返回服务名称，方便本地确认服务已启动。"""
    return {"message": settings.app_name}
