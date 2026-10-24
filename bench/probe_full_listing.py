# -*- coding: utf-8 -*-
"""一次性探针：把「完整文件树」(约 330 万字符) 直接发给模型，看服务端收不收。

这是方案 2「前端只给文件树」的关键问题：整棵树到底塞不塞得进上下文。
一次请求定论，结果写 bench/probe_full_listing.json。
"""
import json
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from retrieval_bench import API_URL, MODEL, SYSTEM_PROMPT, api_key, file_listing, WORKSPACE

OUT = Path("bench/probe_full_listing.json")


def main():
    listing = file_listing(WORKSPACE)
    chars = len(listing)
    body = {"model": MODEL, "max_tokens": 16,
            "system": SYSTEM_PROMPT,
            "messages": [{"role": "user", "content": listing + "\n\n问题：测试"}]}
    raw = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(API_URL, data=raw, method="POST", headers={
        "Content-Type": "application/json",
        "x-api-key": api_key(),
        "authorization": "Bearer " + api_key(),
        "anthropic-version": "2023-06-01",
    })
    out = {"字符数": chars, "字节数": len(raw), "条目数": len(listing.splitlines()),
           "估算tokens_按3字符": chars // 3}
    t0 = time.time()
    try:
        with urllib.request.urlopen(req, timeout=900) as resp:
            payload = json.loads(resp.read().decode("utf-8"))
            out["结果"] = "通过"
            out["HTTP"] = resp.status
            out["服务端报input_tokens"] = (payload.get("usage") or {}).get("input_tokens")
    except urllib.error.HTTPError as e:
        out["结果"] = "被拒"
        out["HTTP"] = e.code
        out["错误"] = e.read().decode("utf-8", "replace")[:600]
    except Exception as exc:  # noqa: BLE001
        out["结果"] = "异常"
        out["错误"] = "%s: %s" % (type(exc).__name__, exc)
    out["秒"] = round(time.time() - t0, 1)
    print(json.dumps(out, ensure_ascii=False, indent=2))
    OUT.write_text(json.dumps(out, ensure_ascii=False, indent=2), encoding="utf-8")


if __name__ == "__main__":
    main()
