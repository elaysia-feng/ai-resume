# -*- coding: utf-8 -*-
"""SQLite 任务队列的跨语言端到端验证。

Python 和 Java 指向同一个 SQLite 文件（路径由 CROSS_TEST_DB 环境变量给出），
两边对「表的字段名」和「消息体的字段名」的理解必须完全一致，
否则一边投的消息另一边解析不了、会被静默丢进死信流。

三步跑，两步之间夹一次 Java 的集成测试，双向都验一遍：

    # 0. 指定两端共用的库文件
    $env:CROSS_TEST_DB = 'F:\\myInterestingProgram\\AI-Resume-Forge\\.tmp-cross-test.db'

    # 1. Python 侧投递到 agent.resume.run（模拟 Python worker 发任务）
    python bench/verify_stream_e2e.py publish

    # 2. Java 侧：读 Python 投的消息 + 往 test.cross.java.result 投一条
    mvn test -Dtest=AgentStreamCrossLanguageTest -Dstream.cross.check=true

    # 3. Python 侧读 Java 投的消息
    python bench/verify_stream_e2e.py consume
"""
import asyncio
import json
import os
import sqlite3
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "backend" / "python"))

from src.app.stream import agent_stream  # noqa: E402
from src.app.worker.agent_run_worker import AgentRunJobMessage  # noqa: E402

DB_ENV = "CROSS_TEST_DB"
SCHEMA_PATH = ROOT / "backend" / "java" / "db_schema" / "schema.sql"

RESUME_STREAM = "agent.resume.run"
RESUME_GROUP = "agent-resume-worker"
RESULT_STREAM = "test.cross.java.result"
RESULT_GROUP = "python-cross-reader"
DLQ_STREAM = "agent.run.dlq"
DLQ_GROUP = "java-dlq-reader"

# Java 侧 AgentRunJobMessage 序列化出来的 JSON（和 Java 集成测试里的断言一致）
JAVA_PAYLOAD = {
    "jobType": "START",
    "runId": 4242,
    "sessionId": 11,
    "resumeId": 22,
    "sceneCode": "JD_CUSTOMIZE",
    "targetSectionId": 33,
    "userInput": "帮我优化项目经历",
    "jobDescription": "Java 后端工程师",
    "answers": [],
}

MARKER_RUN_ID = 999001

ok_count = 0
fail_count = 0


def check(label: str, condition: bool, detail: str = "") -> None:
    global ok_count, fail_count
    if condition:
        ok_count += 1
        print("  [OK] %s" % label)
    else:
        fail_count += 1
        print("  [FAIL] %s %s" % (label, detail))


def open_db() -> sqlite3.Connection:
    """打开两端共用的 SQLite 文件并确保 schema 存在。"""
    db_path = os.environ.get(DB_ENV)
    if not db_path:
        print("环境变量 %s 没设置（指向 Java 和 Python 共用的库文件）" % DB_ENV)
        raise SystemExit(2)
    path = Path(db_path)
    if str(path.parent) not in ("", "."):
        path.parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(str(path), timeout=10, isolation_level=None)
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("PRAGMA busy_timeout=10000")
    conn.execute("PRAGMA foreign_keys=ON")
    conn.executescript(SCHEMA_PATH.read_text(encoding="utf-8"))
    return conn


def phase_publish(conn: sqlite3.Connection) -> None:
    print("== 阶段1: Python 投递到 %s ==" % RESUME_STREAM)
    agent_stream.ensure_group(conn, RESUME_STREAM, RESUME_GROUP)

    agent_stream.publish(conn, RESUME_STREAM, RESUME_GROUP, JAVA_PAYLOAD)
    print("  已投递 Java 格式的任务消息")

    # 自己读回来，验证 Python 自己的收发闭环
    messages = agent_stream.read_batch(conn, RESUME_STREAM, RESUME_GROUP, "py-reader", 10)
    found = False
    for message_id, raw, _retry in messages:
        job = AgentRunJobMessage.model_validate(json.loads(raw))
        if job.run_id == JAVA_PAYLOAD["runId"]:
            found = True
        agent_stream.ack(conn, message_id)
    check("Python 投出的消息能被自己的模型解析并读到", found)

    # 坏消息走死信流
    conn.execute(
        "INSERT INTO agent_task (stream, group_name, payload, status, created_at, updated_at)"
        " VALUES (?, ?, ?, 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
        (RESUME_STREAM, RESUME_GROUP, "{不是合法 JSON"),
    )
    messages = agent_stream.read_batch(conn, RESUME_STREAM, RESUME_GROUP, "py-reader", 10)
    bad = None
    for message_id, raw, _retry in messages:
        try:
            AgentRunJobMessage.model_validate(json.loads(raw))
        except Exception:
            bad = message_id
            agent_stream.to_dead_letter(
                conn, DLQ_STREAM, DLQ_GROUP, message_id, raw, "invalid_payload"
            )
            agent_stream.ack(conn, message_id)
    check("坏消息被识别出来", bad is not None)

    reasons = [
        r[0]
        for r in conn.execute(
            "SELECT dlq_reason FROM agent_task WHERE stream = ? AND dlq_reason IS NOT NULL",
            (DLQ_STREAM,),
        ).fetchall()
    ]
    check("坏消息进了死信流并带原因", "invalid_payload" in reasons, str(reasons))

    # 给 Java 侧留一条带标记的消息，等 Java 测试来读
    agent_stream.publish(conn, RESUME_STREAM, RESUME_GROUP, {
        "jobType": "START",
        "runId": MARKER_RUN_ID,
        "sessionId": 1,
        "resumeId": 2,
        "sceneCode": "JD_CUSTOMIZE",
        "targetSectionId": 3,
        "userInput": "python -> java 标记",
        "jobDescription": "JD",
        "answers": [],
    })
    print("  已留下 runId=%d 的标记消息等 Java 侧读" % MARKER_RUN_ID)


def phase_consume(conn: sqlite3.Connection) -> None:
    print("== 阶段3: Python 读 Java 投到 %s 的消息 ==" % RESULT_STREAM)
    agent_stream.ensure_group(conn, RESULT_STREAM, RESULT_GROUP)
    messages = agent_stream.read_batch(conn, RESULT_STREAM, RESULT_GROUP, "py-reader", 20)
    found = None
    for message_id, raw, _retry in messages:
        payload = json.loads(raw)
        if payload.get("runId") == 4242:
            found = payload
        agent_stream.ack(conn, message_id)
    check("Python 读到了 Java 投的 runId=4242 消息", found is not None)
    if found:
        check("消息体字段名与 Java 一致（jobType/runId/sceneCode）",
              set(found.keys()) >= {"jobType", "runId", "sceneCode"},
              str(sorted(found.keys())))
        job = AgentRunJobMessage.model_validate(found)
        check("Java 投的消息能被 Python 的模型解析",
              job.run_id == 4242 and job.job_type == "START",
              "run_id=%s job_type=%s" % (job.run_id, job.job_type))


def main() -> int:
    phase = sys.argv[1] if len(sys.argv) > 1 else "publish"
    conn = open_db()
    print("SQLite: %s" % os.environ.get(DB_ENV))
    if phase == "publish":
        phase_publish(conn)
    elif phase == "consume":
        phase_consume(conn)
    else:
        print("用法: verify_stream_e2e.py [publish|consume]")
        return 2
    conn.close()
    print("\n结果: %d 通过 / %d 失败" % (ok_count, fail_count))
    return 0 if fail_count == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
