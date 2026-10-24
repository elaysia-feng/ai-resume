"""pytest 全局配置：把测试用的 SQLite 路径指到临时目录。

graph 现在挂了真的 checkpointer（以前 langgraph-checkpoint-redis 没装，
build_checkpointer() 一直返回 None，checkpoint 从来没生效过）。
不隔离的话，测试会把开发者本地 checkpoints.db 里的状态恢复出来，污染断言。
"""
import os
import tempfile
from pathlib import Path

_TMP_DIR = Path(tempfile.mkdtemp(prefix="arf_test_"))

# 环境变量优先级高于 .env，所以这里能覆盖 .env 里配的路径
os.environ.setdefault("CHECKPOINT_DB_PATH", str(_TMP_DIR / "checkpoints.db"))
os.environ.setdefault("AGENT_TASK_DB_PATH", str(_TMP_DIR / "ai_resume.db"))
