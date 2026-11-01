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
    """发送一条 Java API 请求并返回状态码与响应文本。

    Args:
        method: HTTP 方法。
        path: API 路径。
        body: 可选 JSON 请求体。
        token: 可选本机 API 令牌。

    Returns:
        tuple[int, str]: HTTP 状态码与响应文本。
    """
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
    """发送流式聊天请求并按 SSE 事件边界解析响应。

    Args:
        path: SSE API 路径。
        body: 请求体。
        token: 本机 API 令牌。

    Returns:
        list[tuple[str, str]]: 事件名称与事件数据。
    """
    req = urllib.request.Request(
        BASE + path, data=json.dumps(body).encode("utf-8"),
        headers={"Content-Type": "application/json", "Authorization": "Bearer " + token}, method="POST")
    events = []
    with urllib.request.urlopen(req, timeout=900) as resp:
        # 1. 累积当前事件的 event 与 data 字段。
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
    # 2. 返回解析后的事件序列供基准计分。
    return events


def main():
    """运行聊天工具调用题目并保存回答记录。

    Returns:
        int: 全部题目完成时返回 0。
    """
    # 1. 创建本机数据会话并准备聊天工作区。
    runs = int(sys.argv[1]) if len(sys.argv) > 1 else 1
    suffix = str(int(time.time() * 1000))[-9:]
    username = "javabench_" + suffix
    _, payload = http("POST", "/api/auth/local-profile", {"displayName": username})
    token = json.loads(payload)["data"]["token"]

    _, payload = http("POST", "/api/chat/workspace", {"path": str(WORKSPACE)}, token)
    print("工作区:", json.loads(payload).get("data"))

    # 2. 逐轮运行问题、累计得分并记录工具调用。
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

    print("\n==== Java Chat API + Python Agent 各轮得分: %s ====" %
          ", ".join("%d/%d" % t for t in totals))
    # 3. 保存回答，便于后续人工复核。
    Path("bench/answers_java.md").write_text(
        "\n\n".join("## 第%d轮 %s\n\n```\n%s\n```" % (o["run"], o["question"], o["answer"].strip())
                    for o in out),
        encoding="utf-8")
    return 0


if __name__ == "__main__":
    sys.exit(main())
