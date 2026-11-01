# -*- coding: utf-8 -*-
"""运行时验收：后端跑在临时 SQLite 上，走一遍本机业务链路。

覆盖：
1. 用 db_schema/schema.sql 建库
2. 启动 Java 后端（jar）
3. 创建本地数据会话（写 users 表，自增主键 + LocalDateTime 自动填充）
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
import tempfile
import time
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BACKEND = ROOT / "backend" / "java"
DB_FILE = Path(tempfile.gettempdir()) / ("arf_runtime_%s.db" % os.getpid())
SCHEMA = BACKEND / "db_schema" / "schema.sql"
BASE = os.environ.get("AI_RESUME_TEST_BASE_URL", "http://127.0.0.1:18080")

results = []


def check(label, cond, detail=""):
    """记录运行时断言结果。

    Args:
        label: 校验项名称。
        cond: 校验是否通过。
        detail: 失败时显示的补充信息。
    """
    results.append((label, bool(cond), detail))
    print("  [%s] %s %s" % ("OK" if cond else "FAIL", label, detail))


def http(method, path, body=None, token=None):
    """调用临时 Java 服务并返回 HTTP 状态码和 JSON 响应。

    Args:
        method: HTTP 方法。
        path: 请求路径。
        body: 可选 JSON 请求体。
        token: 可选本机 API 测试令牌。

    Returns:
        tuple[int, dict]: 状态码与响应对象。
    """
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
    """等待临时 Java 服务启动或进程提前退出。

    Args:
        proc: Java 后端子进程。
        timeout: 最长等待秒数。

    Returns:
        bool: 服务可响应时返回 True。
    """
    # 1. 在限时内确认 Java 进程仍在运行。
    deadline = time.time() + timeout
    while time.time() < deadline:
        if proc.poll() is not None:
            return False
        # 2. 用受保护接口探活；401 也表明服务已启动。
        try:
            with urllib.request.urlopen(BASE + "/api/auth/me", timeout=5) as resp:
                resp.read()
                return True
        except urllib.error.HTTPError:
            return True
        except Exception:
            time.sleep(1)
    return False


def main() -> int:
    """使用临时 SQLite 验证本地会话和简历写入链路。

    Returns:
        int: 全部检查通过时返回 0，否则返回 1。
    """
    jar = BACKEND / "target" / "ai-resume-forge-backend.jar"
    if not jar.is_file():
        print("jar 不存在，先构建")
        return 1

    # 1. 创建只供本次运行使用的 SQLite 文件。
    DB_FILE.parent.mkdir(parents=True, exist_ok=True)
    if DB_FILE.exists():
        DB_FILE.unlink()
    conn = sqlite3.connect(str(DB_FILE))
    conn.executescript(SCHEMA.read_text(encoding="utf-8"))
    conn.commit()
    conn.close()
    print("建库完成: %s" % DB_FILE)

    # 2. 启动 Java 后端并注入测试用令牌，避免读取真实凭据。
    env = dict(os.environ)
    env["DB_FILE"] = str(DB_FILE)
    env["SERVER_PORT"] = str(BASE.rsplit(":", 1)[-1])
    env["JWT_SECRET"] = "test-only-jwt-secret-with-more-than-thirty-two-bytes"
    env["INTERNAL_AGENT_SERVICE_TOKEN"] = "test-only-internal-agent-token"
    log_path = DB_FILE.with_suffix(".log")
    log = open(log_path, "w", encoding="utf-8")
    proc = subprocess.Popen(
        ["java", "-jar", str(jar)],
        cwd=str(BACKEND), env=env, stdout=log, stderr=subprocess.STDOUT,
    )
    try:
        if not wait_ready(proc):
            print("后端没起来")
            print(log_path.read_text(encoding="utf-8", errors="replace")[-3000:])
            return 1
        print("后端已就绪")

        # 3. 创建本地数据会话并取得本机 API 令牌。
        import time as _t
        stamp = str(int(_t.time() * 1000))[-9:]
        username = "rt_" + stamp
        status, payload = http("POST", "/api/auth/local-profile", {
            "displayName": username,
        })
        check("本地数据会话创建成功", status == 200, str(payload)[:200])
        token = (payload.get("data") or {}).get("token")
        check("拿到 JWT", bool(token))
        if not token:
            return 1

        # 4. 创建简历、读取列表并新增章节。
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

        # 5. 直接查临时数据库核对生成的记录。
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

        # 5.1 任务队列表存在并包含运行所需字段。
        cols = [r[1] for r in conn.execute("PRAGMA table_info(agent_task)").fetchall()]
        check("agent_task 表结构正确",
              set(["stream", "group_name", "payload", "status"]).issubset(set(cols)), str(cols))

        # 5.2 验证目标 schema 的表数量。
        tables = [r[0] for r in conn.execute(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")]
        check("表数量符合预期（13 张：12 业务表 + agent_stream_group）",
              len(tables) == 13, str(len(tables)))
        conn.close()
    finally:
        # 6. 停止子进程并清理临时数据库与日志。
        if proc.poll() is None:
            proc.send_signal(signal.SIGTERM)
            try:
                proc.wait(timeout=20)
            except subprocess.TimeoutExpired:
                proc.kill()
        log.close()
        for suffix in ("", "-wal", "-shm"):
            Path(str(DB_FILE) + suffix).unlink(missing_ok=True)
        log_path.unlink(missing_ok=True)

    failed = [r for r in results if not r[1]]
    print("\n结果: %d 通过 / %d 失败" % (len(results) - len(failed), len(failed)))
    return 0 if not failed else 1


if __name__ == "__main__":
    sys.exit(main())
