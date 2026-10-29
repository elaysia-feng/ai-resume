"""agent_stream 队列工具的单元测试（内存 SQLite，不依赖真实库文件）。"""

import sqlite3
from datetime import UTC, datetime, timedelta

from app.stream.agent_stream import (
    STATUS_CLAIMED,
    STATUS_PENDING,
    publish,
    read_batch,
    reclaim_stale,
)

DDL = """
CREATE TABLE agent_task (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  stream TEXT NOT NULL,
  group_name TEXT NOT NULL,
  payload TEXT NOT NULL,
  status TEXT NOT NULL,
  retry_count INTEGER DEFAULT 0,
  dlq_reason TEXT,
  claim_token TEXT,
  consumer TEXT,
  claimed_at TEXT,
  created_at TEXT,
  updated_at TEXT
)
"""


def _new_client() -> sqlite3.Connection:
    client = sqlite3.connect(":memory:", isolation_level=None)
    client.execute(DDL)
    return client


def _fmt(dt: datetime) -> str:
    """SQLite CURRENT_TIMESTAMP 同款 UTC 格式，保证 julianday 能解析。"""
    return dt.astimezone(UTC).strftime("%Y-%m-%d %H:%M:%S")


def test_reclaim_stale_returns_crashed_claim_to_pending():
    client = _new_client()
    message_id = publish(client, "s", "g", {"hello": "world"})
    read_batch(client, "s", "g", "c1", 1)
    # 模拟 worker 崩溃残留：认领时间拨回 2 小时前
    client.execute(
        "UPDATE agent_task SET claimed_at = ? WHERE id = ?",
        (_fmt(datetime.now(UTC) - timedelta(hours=2)), int(message_id)),
    )

    reclaimed = reclaim_stale(client, "s", "g", timeout_seconds=1800)

    assert reclaimed == 1
    status = client.execute("SELECT status FROM agent_task WHERE id = ?", (int(message_id),)).fetchone()[0]
    assert status == STATUS_PENDING
    # 回收后应能被重新认领消费
    again = read_batch(client, "s", "g", "c2", 1)
    assert len(again) == 1 and again[0][0] == message_id


def test_reclaim_stale_keeps_fresh_claim():
    client = _new_client()
    publish(client, "s", "g", {"hello": "world"})
    read_batch(client, "s", "g", "c1", 1)  # 刚认领，未超时

    reclaimed = reclaim_stale(client, "s", "g", timeout_seconds=1800)

    assert reclaimed == 0
    status = client.execute("SELECT status FROM agent_task").fetchone()[0]
    assert status == STATUS_CLAIMED


def test_reclaim_stale_ignores_other_streams():
    client = _new_client()
    publish(client, "other-stream", "g", {"hello": "world"})
    stale_claimed_at = _fmt(datetime.now(UTC) - timedelta(hours=2))
    client.execute(
        "UPDATE agent_task SET status = ?, claimed_at = ?",
        (STATUS_CLAIMED, stale_claimed_at),
    )

    assert reclaim_stale(client, "s", "g", timeout_seconds=1800) == 0
