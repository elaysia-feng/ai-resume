"""桌面端打包入口：把 FastAPI 服务打成独立 exe。

用法（开发调试）：
    python scripts/desktop_entry.py

由 PyInstaller 打成 exe 后，桌面端主进程直接 spawn 这个 exe，
用户机器上不需要装 Python。

环境变量由 desktop 主进程注入（见 desktop/electron/backend_process.ts）：
- AI_RESUME_AGENT_TASK_DB_PATH / AGENT_TASK_DB_PATH：与 Java 共用的 SQLite 业务库
- LLM_PROVIDER / LLM_MODEL_PROVIDER / LLM_API_KEY / LLM_BASE_URL：统一模型配置
- LLM_EXTRA_HEADERS / LLM_PROVIDER_OPTIONS：中转站请求头和集成原生参数
- JAVA_INTERNAL_BASE_URL / INTERNAL_AGENT_SERVICE_TOKEN：调 Java 内部接口
"""

import os
import sys
from pathlib import Path

# 开发模式直接运行脚本时，也能从任意工作目录导入 src/app 包。
project_root = Path(__file__).resolve().parents[1]
source_root = project_root / "src"
if not getattr(sys, "frozen", False) and str(source_root) not in sys.path:
    sys.path.insert(0, str(source_root))

import uvicorn  # noqa: E402

from app.main import app  # noqa: E402


def main() -> None:
    host = os.getenv("AI_RESUME_PYTHON_HOST", "127.0.0.1")
    port = int(os.getenv("AI_RESUME_PYTHON_PORT", "8000"))
    uvicorn.run(app, host=host, port=port, log_level="info")


if __name__ == "__main__":
    main()
