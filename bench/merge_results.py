# -*- coding: utf-8 -*-
"""把分批跑的基准结果合并成一份 bench/results_final.json。"""
import json
import sys
from pathlib import Path

OUT = Path("bench/results_final.json")

parts = []
for p in sys.argv[1:]:
    f = Path(p)
    if not f.is_file():
        print("跳过（不存在）: %s" % p)
        continue
    data = json.loads(f.read_text(encoding="utf-8"))
    print("读入 %s -> %d 个实验臂" % (p, len(data.get("results", []))))
    parts.append(data)

if not parts:
    print("没有可合并的结果")
    sys.exit(1)

merged = {"workspace": parts[0].get("workspace"), "model": parts[0].get("model"), "results": []}
for data in parts:
    for r in data.get("results", []):
        if not any(x.get("name") == r.get("name") for x in merged["results"]):
            merged["results"].append(r)

OUT.write_text(json.dumps(merged, ensure_ascii=False, indent=2), encoding="utf-8")
print("\n==== 合并结果 %s ====" % OUT)
print("%-38s | %-4s | %-5s | %-6s | %-9s | %-8s | %s"
      % ("实验臂", "状态", "调用", "读字节", "prompt", "输出", "得分"))
for r in merged["results"]:
    hit = sum(s["命中"] for s in r.get("scores", {}).values())
    full = sum(s["总分"] for s in r.get("scores", {}).values())
    print("%-38s | %-4s | %-5d | %-6d | %-9d | %-8d | %d/%d"
          % (r.get("name", "")[:38], "OK" if r.get("ok") else "失败",
             r.get("llm_calls", 0), r.get("read_bytes", 0),
             r.get("input_tokens", 0), r.get("output_tokens", 0), hit, full))
    if r.get("error"):
        print("      错误: %s" % str(r["error"])[:160])
