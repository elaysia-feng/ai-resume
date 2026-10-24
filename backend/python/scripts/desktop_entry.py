"""桌面端打包入口：把 FastAPI 服务打成独立 exe。

用法（开发调试）：
    python scripts/desktop_entry.py

由 PyInstaller 打成 exe 后，桌面端主进程直接 spawn 这个 exe，
用户机器上不需要装 Python。

环境变量由 desktop 主进程注入（见 desktop/electron/backend.ts）：
- AI_RESUME_AGENT_TASK_DB_PATH / AGENT_TASK_DB_PATH：与 Java 共用的 SQLite 业务库
- LLM_PROVIDER / DEEPSEEK_API_KEY / MIMO_API_KEY ...：模型配置
- JAVA_INTERNAL_BASE_URL / INTERNAL_AGENT_SERVICE_TOKEN：调 Java 内部接口
"""

import os
import sys

# 打包成 exe 后 sys.path 里没有源码目录，要先把项目根加进去，
# 否则 `from src.app...` 会 ModuleNotFoundError。
if getattr(sys, "frozen", False):
    project_root = os.path.dirname(os.path.dirname(os.path.abspath(sys.executable)))
    if project_root not in sys.path:
        sys.path.insert(0, project_root)

import uvicorn  # noqa: E402

from src.app.main import app  # noqa: E402


def main() -> None:
    host = os.getenv("AI_RESUME_PYTHON_HOST", "127.0.0.1")
    port = int(os.getenv("AI_RESUME_PYTHON_PORT", "8000"))
    uvicorn.run(app, host=host, port=port, log_level="info")


if __name__ == "__main__":
    main()
