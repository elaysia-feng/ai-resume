# -*- coding: utf-8 -*-
"""运行时验收：后端跑在 SQLite 上，走一遍真实业务链路。

覆盖：
1. 用 db_schema/schema.sql 建库
2. 启动 Java 后端（jar）
3. 注册用户（写 users 表，自增主键 + LocalDateTime 自动填充）
4. 读简历列表 / 加一个章节 / 读详情（写 resumes / resume_sections）
5. 直接开 SQLite 查库，核对自增 ID、时间戳、JSON 字段真的落库了
6. 关掉后端
"""
import json
import os
import signal
import sqlite3
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(r"F:\myInterestingProgram\AI-Resume-Forge")
BACKEND = ROOT / "backend" / "java"
DB_FILE = BACKEND / "data" / "ai_resume.db"
SCHEMA = BACKEND / "db_schema" / "schema.sql"
BASE = "http://127.0.0.1:8080"

results = []


def check(label, cond, detail=""):
    results.append((label, bool(cond), detail))
    print("  [%s] %s %s" % ("OK" if cond else "FAIL", label, detail))


def http(method, path, body=None, token=None):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(BASE + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8") or "{}")
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode("utf-8") or "{}")


def wait_ready(proc, timeout=120):
    deadline = time.time() + timeout
    while time.time() < deadline:
        if proc.poll() is not None:
            return False
        try:
            with urllib.request.urlopen(BASE + "/api/auth/me", timeout=5) as resp:
                resp.read()
                return True
        except urllib.error.HTTPError:
            return True   # 401 说明服务在跑，只是没带 token
        except Exception:
            time.sleep(1)
    return False


def main() -> int:
    # ---- 1. 建库 ----
    DB_FILE.parent.mkdir(parents=True, exist_ok=True)
    if DB_FILE.exists():
        DB_FILE.unlink()
    conn = sqlite3.connect(str(DB_FILE))
    conn.executescript(SCHEMA.read_text(encoding="utf-8"))
    conn.commit()
    conn.close()
    print("建库完成: %s" % DB_FILE)

    # ---- 2. 启动后端 ----
    jar = BACKEND / "target" / "ai-resume-forge-backend.jar"
    if not jar.is_file():
        print("jar 不存在，先构建")
        return 1
    env = dict(os.environ)
    env["DB_FILE"] = str(DB_FILE)
    log = open(ROOT / ".tmp-backend.log", "w", encoding="utf-8")
    proc = subprocess.Popen(
        ["java", "-jar", str(jar)],
        cwd=str(BACKEND), env=env, stdout=log, stderr=subprocess.STDOUT,
    )
    try:
        if not wait_ready(proc):
            print("后端没起来")
            print((ROOT / ".tmp-backend.log").read_text(encoding="utf-8", errors="replace")[-3000:])
            return 1
        print("后端已就绪")

        # ---- 3. 注册用户 ----
        import time as _t
        stamp = str(int(_t.time() * 1000))[-9:]
        username = "rt_" + stamp
        status, payload = http("POST", "/api/auth/register", {
            "username": username, "email": username + "@example.com", "password": "Passw0rd!2026",
        })
        check("注册接口返回 200", status == 200, str(payload)[:200])
        token = (payload.get("data") or {}).get("token")
        check("拿到 JWT", bool(token))
        if not token:
            return 1

        # ---- 4. 简历链路 ----
        status, payload = http("POST", "/api/resumes", {"title": "SQLite 验收简历"}, token=token)
        check("建简历成功", status == 200, str(payload)[:200])

        status, payload = http("GET", "/api/resumes", token=token)
        check("简历列表可读", status == 200, str(payload)[:200])
        resumes = payload.get("data") or []
        check("简历列表能查到刚建的", len(resumes) >= 1, "数量=%d" % len(resumes))
        if not resumes:
            return 1
        resume_id = resumes[0].get("id")

        status, payload = http("POST", "/api/resumes/%s/sections" % resume_id, {
            "sectionCode": "PROJECTS", "sectionTitle": "项目经历",
            "sectionType": "CUSTOM", "schemaType": "LIST",
            "contentJson": json.dumps({
                "items": [{
                    "name": "SQLite 运行时验收",
                    "role": "后端开发",
                    "dateRange": "2026.01-2026.02",
                    "description": "把 MySQL 和 Redis 换成 SQLite 后跑通全链路",
                }],
            }, ensure_ascii=False),
        }, token=token)
        check("新增章节成功", status == 200, str(payload)[:300])

        status, payload = http("GET", "/api/resumes/%s" % resume_id, token=token)
        check("简历详情可读", status == 200, str(payload)[:120])
        sections = payload.get("data", {}).get("sections") or []
        check("章节能读回来", len(sections) >= 1, "数量=%d" % len(sections))

        # ---- 5. 直接查库核对 ----
        conn = sqlite3.connect(str(DB_FILE))
        conn.row_factory = sqlite3.Row
        user = conn.execute("SELECT * FROM users WHERE username = ?", (username,)).fetchone()
        check("用户真的落库了", user is not None)
        if user:
            check("users.id 是自增整数", isinstance(user["id"], int) and user["id"] > 0, str(user["id"]))
            check("created_at 有值且是字符串时间", bool(user["created_at"]), repr(user["created_at"]))
            check("updated_at 有值", bool(user["updated_at"]), repr(user["updated_at"]))
            check("enabled 是布尔", user["enabled"] in (0, 1), repr(user["enabled"]))

        rows = conn.execute(
            "SELECT * FROM resume_sections WHERE resume_id = ? ORDER BY id", (resume_id,)
        ).fetchall()
        check("章节行落库", len(rows) >= 1, "数量=%d" % len(rows))
        if rows:
            check("content_json 是合法 JSON",
                  isinstance(json.loads(rows[0]["content_json"]), dict),
                  rows[0]["content_json"][:60])

        # 验证码表存在（Redis 移除后验证码改存这里）
        cols = [r[1] for r in conn.execute("PRAGMA table_info(verification_code)").fetchall()]
        check("verification_code 表结构正确",
              set(["email", "code", "expires_at"]).issubset(set(cols)), str(cols))

        # 任务队列表存在
        cols = [r[1] for r in conn.execute("PRAGMA table_info(agent_task)").fetchall()]
        check("agent_task 表结构正确",
              set(["stream", "group_name", "payload", "status"]).issubset(set(cols)), str(cols))

        # 真的没有 MySQL 残留
        tables = [r[0] for r in conn.execute(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")]
        check("表数量符合预期（14 张：13 业务表 + agent_stream_group）",
              len(tables) == 14, str(len(tables)))
        conn.close()
    finally:
        proc.send_signal(signal.SIGTERM)
        try:
            proc.wait(timeout=20)
        except subprocess.TimeoutExpired:
            proc.kill()
        log.close()

    failed = [r for r in results if not r[1]]
    print("\n结果: %d 通过 / %d 失败" % (len(results) - len(failed), len(failed)))
    return 0 if not failed else 1


if __name__ == "__main__":
    sys.exit(main())
