"""SQLite 任务队列工具。

Java 侧用 JdbcTemplate 读写同一张 agent_task 表，这里是 Python 侧的对等实现。
消息统一放在单个字段 payload 里，内容是整条消息的 JSON 字符串。

和 Java 侧 com.elias.common.stream.AgentStreamClient 一一对应：
- 表：agent_task / agent_stream_group
- 字段：payload / retryCount / dlqReason
- 认领：UPDATE ... WHERE id IN (SELECT ...) 原子认领 + claim_token 回读

跨进程说明：Java 后端和 Python worker 指向同一个 SQLite 文件，
两边都开 WAL + busy_timeout，读写互不阻塞。
"""

import json
import sqlite3
import uuid
from pathlib import Path
from typing import Any

from src.app.config.settings import get_settings

# 消息体字段名，必须和 Java 侧 AgentStreamClient 保持一致。
FIELD_PAYLOAD = "payload"
FIELD_RETRY_COUNT = "retryCount"
FIELD_DLQ_REASON = "dlqReason"

STATUS_PENDING = "PENDING"
STATUS_CLAIMED = "CLAIMED"
STATUS_DONE = "DONE"

_conn: sqlite3.Connection | None = None


def get_client() -> sqlite3.Connection:
    """全局共享一个 SQLite 连接。

    isolation_level=None 走 autocommit：每条语句自带隐式事务，
    认领用的 UPDATE 才是原子的（不会被两个 worker 同时拿到同一条）。
    """
    global _conn
    if _conn is None:
        settings = get_settings()
        path = Path(settings.agent_task_db_path)
        if str(path.parent) not in ("", "."):
            path.parent.mkdir(parents=True, exist_ok=True)
        _conn = sqlite3.connect(str(path), timeout=10, isolation_level=None)
        _conn.execute("PRAGMA journal_mode=WAL")
        _conn.execute("PRAGMA busy_timeout=10000")
        _conn.execute("PRAGMA foreign_keys=ON")
    return _conn


def ensure_group(client: sqlite3.Connection, stream: str, group: str) -> None:
    """确保消费组已登记（幂等）。

    Args:
        client: SQLite 连接。
        stream: 任务流名。
        group: 消费组名。
    """
    client.execute(
        "INSERT OR IGNORE INTO agent_stream_group (stream, group_name) VALUES (?, ?)",
        (stream, group),
    )


def publish(client: sqlite3.Connection, stream: str, group: str, payload: dict[str, Any]) -> str:
    """投递一条 JSON 消息，返回任务 ID。

    Args:
        client: SQLite 连接。
        stream: 任务流名。
        group: 目标消费组（生产者和消费者必须约定好这一组属于谁）。
        payload: 消息体，会被序列化成 JSON。

    Returns:
        新任务的自增 ID。
    """
    body = json.dumps(payload, ensure_ascii=False)
    cursor = client.execute(
        "INSERT INTO agent_task (stream, group_name, payload, status, created_at, updated_at)"
        " VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
        (stream, group, body, STATUS_PENDING),
    )
    return str(cursor.lastrowid)


def read_batch(
    client: sqlite3.Connection,
    stream: str,
    group: str,
    consumer: str,
    count: int,
) -> list[tuple[str, str, int]]:
    """认领并读取一批新消息。

    Args:
        client: SQLite 连接。
        stream: 任务流名。
        group: 消费组名。
        consumer: 消费者名（同组多实例时要区分）。
        count: 最多认领多少条。

    Returns:
        [(任务 ID, 消息体 JSON, 已重试次数), ...]
    """
    token = uuid.uuid4().hex
    client.execute(
        "UPDATE agent_task SET status = ?, consumer = ?, claim_token = ?,"
        " claimed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP"
        " WHERE id IN (SELECT id FROM agent_task"
        "   WHERE stream = ? AND group_name = ? AND status = ?"
        "   ORDER BY id LIMIT ?)",
        (STATUS_CLAIMED, consumer, token, stream, group, STATUS_PENDING, count),
    )
    rows = client.execute(
        "SELECT id, payload, retry_count FROM agent_task WHERE claim_token = ? ORDER BY id",
        (token,),
    ).fetchall()
    return [(str(row[0]), row[1], int(row[2])) for row in rows]


def ack(client: sqlite3.Connection, message_id: str) -> None:
    """确认消息已处理完成。

    Args:
        client: SQLite 连接。
        message_id: 任务 ID。
    """
    client.execute(
        "UPDATE agent_task SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
        (STATUS_DONE, int(message_id)),
    )


def to_dead_letter(
    client: sqlite3.Connection,
    dead_letter_stream: str,
    dead_letter_group: str,
    message_id: str,
    payload: str,
    reason: str,
) -> None:
    """把坏消息转投到死信流，并把原任务标记完成。

    队列没有死信交换机那种自动机制，坏消息要由消费方主动写进来。

    Args:
        client: SQLite 连接。
        dead_letter_stream: 死信流名。
        dead_letter_group: 死信流对应的消费组。
        message_id: 原任务 ID。
        payload: 原消息体 JSON。
        reason: 进入死信的原因。
    """
    client.execute(
        "INSERT INTO agent_task (stream, group_name, payload, status, dlq_reason, created_at, updated_at)"
        " VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
        (dead_letter_stream, dead_letter_group, payload, STATUS_PENDING, reason),
    )
    client.execute(
        "UPDATE agent_task SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
        (STATUS_DONE, int(message_id)),
    )


def republish(client: sqlite3.Connection, message_id: str, retry_count: int) -> None:
    """把任务放回待处理队列（用于消费失败后的重试）。

    Args:
        client: SQLite 连接。
        message_id: 任务 ID。
        retry_count: 累计重试次数。
    """
    client.execute(
        "UPDATE agent_task SET status = ?, retry_count = ?, claim_token = NULL, consumer = NULL,"
        " claimed_at = NULL, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
        (STATUS_PENDING, retry_count, int(message_id)),
    )
