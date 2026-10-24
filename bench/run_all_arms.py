# -*- coding: utf-8 -*-
"""逐个实验臂跑（独立子进程），一个崩了不影响后面的，每个臂单独留档。

用法: python bench/run_all_arms.py 1a 5 2b 4 4b 2 1b 3
"""
import json
import subprocess
import sys
from pathlib import Path

PY = sys.executable
HERE = Path("bench").resolve()
ORDER = ["1a", "5", "2b", "4", "4b", "2", "1b", "3"]


def run_one(arm):
    out = HERE / ("results_arm_%s.json" % arm)
    ans = HERE / ("answers_arm_%s.md" % arm)
    cmd = [PY, "-u", str(HERE / "retrieval_bench.py"),
           "--arm", arm, "--out", str(out), "--answers", str(ans)]
    print("\n########## 跑实验臂 %s ##########" % arm, flush=True)
    proc = subprocess.run(cmd, cwd=str(HERE.parent), capture_output=True, text=True,
                          encoding="utf-8", errors="replace", timeout=1800)
    tail = (proc.stdout or "").strip().splitlines()
    for line in tail[-8:]:
        print("  " + line, flush=True)
    if proc.returncode != 0:
        print("  !! 退出码 %s" % proc.returncode, flush=True)
        err = (proc.stderr or "").strip().splitlines()
        for line in err[-6:]:
            print("  ERR " + line, flush=True)
    if not out.is_file():
        print("  !! 未产出结果文件", flush=True)
        return None
    data = json.loads(out.read_text(encoding="utf-8"))
    return data["results"][0]


def main():
    arms = sys.argv[1:] or ORDER
    summary = []
    for arm in arms:
        try:
            r = run_one(arm)
        except subprocess.TimeoutExpired:
            print("  !! 超时", flush=True)
            r = None
        except Exception as exc:  # noqa: BLE001
            print("  !! 异常 %s: %s" % (type(exc).__name__, exc), flush=True)
            r = None
        if r:
            hit = sum(s["命中"] for s in r.get("scores", {}).values())
            full = sum(s["总分"] for s in r.get("scores", {}).values())
            summary.append((arm, r.get("name", ""), hit, full,
                            r.get("llm_calls", 0), r.get("read_bytes", 0),
                            r.get("input_tokens", 0), r.get("output_tokens", 0),
                            r.get("elapsed", 0.0), (r.get("error") or "")[:120]))
            print("  => 得分 %d/%d" % (hit, full), flush=True)
        else:
            summary.append((arm, "", 0, 0, 0, 0, 0, 0, 0.0, "未完成"))

    print("\n\n==== 全部实验臂汇总 ====")
    print("%-4s | %-40s | %-7s | %-5s | %-9s | %-8s | %-7s | %s"
          % ("臂", "名称", "得分", "调用", "prompt", "输出", "秒", "错误"))
    for s in summary:
        print("%-4s | %-40s | %d/%-5d | %-5d | %-9d | %-8d | %-7.1f | %s"
              % (s[0], s[1][:40], s[2], s[3], s[4], s[6], s[7], s[8], s[9]))


if __name__ == "__main__":
    main()
