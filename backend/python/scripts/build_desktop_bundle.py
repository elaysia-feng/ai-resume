"""用 PyInstaller 把 Python AI 运行时打成独立 exe（桌面端安装包用）。

用法：
    python scripts/build_desktop_bundle.py
    python scripts/build_desktop_bundle.py --onedir   # 默认就是 onedir

产物：
    dist/desktop/ai-resume-api/ai-resume-api.exe

桌面端主进程 spawn 这个 exe，用户机器上不需要装 Python。
"""

from __future__ import annotations

import argparse
import shutil
import subprocess
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
ENTRY = PROJECT_ROOT / "scripts" / "desktop_entry.py"
DIST_DIR = PROJECT_ROOT / "dist" / "desktop"
BUILD_DIR = PROJECT_ROOT / "build" / "desktop"

APP_NAME = "ai-resume-api"

# LangChain / LangGraph 大量使用动态 import，PyInstaller 静态分析抓不到，
# 漏了会在运行时才报 ModuleNotFoundError。
HIDDEN_IMPORTS = [
    "langchain_core",
    "langchain_core.tracers",
    "langchain_core.tracers.schemas",
    "langchain_core.language_models",
    "langchain_core.language_models.chat_models",
    "langchain_core.messages",
    "langchain_core.outputs",
    "langchain_core.prompts",
    "langchain_core.runnables",
    "langchain_core.tools",
    "langchain_core.utils",
    "langchain",
    "langchain.agents",
    "langchain.agents.factory",
    "langchain.chat_models",
    "langchain.chat_models.base",
    "langchain_core.utils.function_calling",
    "langgraph",
    "langgraph.graph",
    "langgraph.graph.state",
    "langgraph.checkpoint.sqlite",
    "langgraph.pregel",
    "langgraph.pregel.main",
    "langchain_openai",
    "openai",
    "uvicorn",
    "uvicorn.logging",
    "uvicorn.loops",
    "uvicorn.protocols",
    "uvicorn.protocols.http",
    "uvicorn.protocols.http.auto",
    "uvicorn.lifespan",
    "uvicorn.lifespan.on",
    "sqlalchemy.dialects.sqlite",
    "aiosqlite",
]


def main() -> None:
    parser = argparse.ArgumentParser(description="Build the desktop Python bundle with PyInstaller.")
    parser.add_argument("--clean", action="store_true", help="Remove previous build output first.")
    args = parser.parse_args()

    if args.clean:
        for path in (DIST_DIR, BUILD_DIR):
            if path.exists():
                shutil.rmtree(path)

    command = [
        sys.executable,
        "-m",
        "PyInstaller",
        "--noconfirm",
        "--onedir",
        "--name",
        APP_NAME,
        # 默认 INFO 会打出 6 万多行 "Analyzing hidden import"，日志 I/O 很重，
        # 而且失败时真正的错误行容易被淹没。只留告警以上。
        "--log-level",
        "WARN",
        "--distpath",
        str(DIST_DIR),
        "--workpath",
        str(BUILD_DIR),
        "--specpath",
        str(BUILD_DIR),
        "--paths",
        str(PROJECT_ROOT),
        # 收集子模块，否则动态 import 的包不会被打进去。
        "--collect-submodules",
        "langchain",
        "--collect-submodules",
        "langchain_core",
        "--collect-submodules",
        "langgraph",
        "--collect-submodules",
        "langchain_openai",
    ]
    for module in HIDDEN_IMPORTS:
        command.extend(["--hidden-import", module])

    command.append(str(ENTRY))

    print("[build] " + " ".join(command))
    result = subprocess.run(command, cwd=PROJECT_ROOT, check=False)
    if result.returncode != 0:
        raise SystemExit(f"PyInstaller 失败，退出码 {result.returncode}")

    exe = DIST_DIR / APP_NAME / f"{APP_NAME}.exe"
    if not exe.exists():
        raise SystemExit(f"打包完成但没找到产物：{exe}")
    size_mb = exe.stat().st_size / 1024 / 1024
    print(f"[done] {exe}（{size_mb:.1f} MB）")


if __name__ == "__main__":
    main()
