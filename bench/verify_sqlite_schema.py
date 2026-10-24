# -*- coding: utf-8 -*-
"""验证 db_schema/schema.sql 能在 SQLite 上干净执行，并抽查关键行为。

同时验证 MySQL 时代的断言在 SQLite 下还成立（外键级联、唯一约束、自增主键）。
"""
import os
import sqlite3
import sys
import tempfile
from pathlib import Path

SCHEMA = Path("backend/java/db_schema/schema.sql")

failures = []


def check(label, cond, detail=""):
    if cond:
        print("  [OK] %s" % label)
    else:
        failures.append(label)
        print("  [FAIL] %s %s" % (label, detail))


def main() -> int:
    sql = SCHEMA.read_text(encoding="utf-8")
    tmpdir = tempfile.mkdtemp(prefix="arf_sqlite_")
    db = os.path.join(tmpdir, "ai_resume.db")
    conn = sqlite3.connect(db)
    conn.execute("PRAGMA foreign_keys = ON")
    try:
        conn.executescript(sql)
        conn.commit()
    except Exception as exc:
        print("  [FAIL] schema 执行失败: %s" % exc)
        return 1
    print("schema 执行成功: %s" % db)

    tables = [r[0] for r in conn.execute(
        "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name")]
    print("表: %s" % ", ".join(tables))
    expected = {
        "users", "resumes", "resume_sections", "resume_versions", "resume_version_sections",
        "ai_session", "ai_message", "ai_agent_run", "ai_run_event", "ai_interview_round",
        "user_setting", "verification_code", "agent_task", "agent_stream_group",
    }
    check("表集合符合预期", set(tables) == expected,
          "多=%s 缺=%s" % (set(tables) - expected, expected - set(tables)))

    # 自增主键
    conn.execute("INSERT INTO users (username, email, password) VALUES ('u1','a@b.c','x')")
    cur = conn.execute("SELECT id FROM users WHERE username='u1'")
    row = cur.fetchone()
    check("自增主键可用", row is not None and row[0] == 1, str(row))

    # 唯一约束
    try:
        conn.execute("INSERT INTO users (username, email, password) VALUES ('u1','z@b.c','x')")
        conn.commit()
        check("唯一约束生效", False, "重复 username 没报错")
    except sqlite3.IntegrityError:
        conn.rollback()
        check("唯一约束生效", True)

    # 外键级联删除
    conn.execute("INSERT INTO resumes (user_id, title) VALUES (1, 'r1')")
    conn.execute("INSERT INTO resume_sections (resume_id, section_code, section_type, schema_type)"
                 " VALUES (1, 'EDU', 'SYSTEM', 'TEXT')")
    conn.execute("DELETE FROM resumes WHERE id=1")
    conn.commit()
    n = conn.execute("SELECT COUNT(*) FROM resume_sections").fetchone()[0]
    check("外键级联删除生效", n == 0, "残留 %d 行" % n)

    # 任务队列表
    conn.execute("INSERT INTO agent_task (stream, group_name, payload)"
                 " VALUES ('agent.resume.run', 'agent-resume-worker', '{}')")
    n = conn.execute("SELECT COUNT(*) FROM agent_task WHERE status='PENDING'").fetchone()[0]
    check("agent_task 默认 PENDING", n == 1, "实际 %d" % n)

    # 验证码表
    conn.execute("INSERT INTO verification_code (email, code, expires_at)"
                 " VALUES ('a@b.c','123456','2026-12-31 00:00:00')")
    n = conn.execute("SELECT COUNT(*) FROM verification_code").fetchone()[0]
    check("verification_code 可写", n == 1)

    # 索引都在
    idx = [r[0] for r in conn.execute(
        "SELECT name FROM sqlite_master WHERE type='index' AND name NOT LIKE 'sqlite_%'")]
    check("索引数量合理（>=20）", len(idx) >= 20, "实际 %d" % len(idx))

    conn.close()
    print("\n结果: %d 项失败" % len(failures))
    return 0 if not failures else 1


if __name__ == "__main__":
    sys.exit(main())
