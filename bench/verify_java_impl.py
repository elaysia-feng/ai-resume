# -*- coding: utf-8 -*-
"""把同样的两道题丢给「现网 Java 实现」（chat API + 后端 tool calling），用同一套标准答案打分。

前提：Java 后端已在 8080 跑起来，MySQL 在 3307。
"""
import json
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from retrieval_bench import QUESTIONS, WORKSPACE, score_answer  # noqa: E402

BASE = "http://127.0.0.1:8080"


def http(method, path, body=None, token=None):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    headers = {"Content-Type": "application/json"} if body is not None else {}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(BASE + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            return resp.status, resp.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")


def sse(path, body, token):
    req = urllib.request.Request(
        BASE + path, data=json.dumps(body).encode("utf-8"),
        headers={"Content-Type": "application/json", "Authorization": "Bearer " + token}, method="POST")
    events = []
    with urllib.request.urlopen(req, timeout=900) as resp:
        name, data = None, []
        for raw in resp:
            line = raw.decode("utf-8", "replace").rstrip("\r\n")
            if line == "":
                if data:
                    events.append((name or "message", "\n".join(data)))
                name, data = None, []
                continue
            if line.startswith("event:"):
                name = line[6:].strip()
            elif line.startswith("data:"):
                data.append(line[5:].lstrip())
    return events


def main():
    runs = int(sys.argv[1]) if len(sys.argv) > 1 else 1
    suffix = str(int(time.time() * 1000))[-9:]
    username = "javabench_" + suffix
    _, payload = http("POST", "/api/auth/register", {
        "username": username, "email": username + "@example.com", "password": "Passw0rd!2026"})
    token = json.loads(payload)["data"]["token"]

    _, payload = http("POST", "/api/chat/workspace", {"path": str(WORKSPACE)}, token)
    print("工作区:", json.loads(payload).get("data"))

    out = []
    totals = []
    for run in range(runs):
        _, payload = http("POST", "/api/chat/sessions", {"title": "Java实现基准"}, token)
        sid = json.loads(payload)["data"]["sessionId"]
        total_hit = total_full = 0
        for q in QUESTIONS:
            t0 = time.time()
            events = sse("/api/chat/sessions/%s/messages" % sid, {"content": q["question"]}, token)
            text = "".join(json.loads(d).get("text", "") for e, d in events if e == "text_delta")
            calls = [json.loads(d) for e, d in events if e == "tool_call"]
            results = [json.loads(d) for e, d in events if e == "tool_result"]
            s = score_answer(text, q)
            total_hit += s["命中"]
            total_full += s["总分"]
            print("\n[第%d轮] %s  得分 %d/%d %s  (%.1fs, %d 次工具调用)" % (
                run + 1, q["name"], s["命中"], s["总分"], s["明细"], time.time() - t0, len(calls)))
            for c, r in zip(calls, results):
                print("   → %s %s" % (c.get("name"), json.dumps(c.get("input"), ensure_ascii=False)))
                print("     %s" % str(r.get("summary"))[:120])
            out.append({"run": run + 1, "question": q["name"], "score": s,
                        "calls": calls, "results": results, "answer": text})
        totals.append((total_hit, total_full))
        print("  >>> 第%d轮合计 %d/%d" % (run + 1, total_hit, total_full))

    print("\n==== Java 实现（后端 tool calling）各轮得分: %s ====" %
          ", ".join("%d/%d" % t for t in totals))
    Path("bench/answers_java.md").write_text(
        "\n\n".join("## 第%d轮 %s\n\n```\n%s\n```" % (o["run"], o["question"], o["answer"].strip())
                    for o in out),
        encoding="utf-8")
    return 0


if __name__ == "__main__":
    sys.exit(main())
