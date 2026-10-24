# -*- coding: utf-8 -*-
"""等 MiniMax 额度恢复后自动补跑剩余实验臂（断点续跑）。

用法: python -u bench/wait_and_run.py [臂...]
默认队列: 2b 4b 1b 3 probe
- 已有成功结果的臂自动跳过，中断后重跑不会重复烧额度
- probe = 把完整文件树(约 330 万字符)一次性发给模型，看服务端收不收
每 90 秒探一次额度，恢复后按顺序逐个跑，每个臂单独留档。
"""
import json
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from retrieval_bench import API_URL, MODEL, api_key

HERE = Path("bench").resolve()
POLL_SECONDS = 90
MAX_WAIT_SECONDS = 55 * 60
QUEUE = ["2b", "4b", "1b", "3", "probe"]


def quota_ok():
    body = {"model": MODEL, "max_tokens": 8, "messages": [{"role": "user", "content": "hi"}]}
    req = urllib.request.Request(API_URL, data=json.dumps(body).encode("utf-8"), method="POST",
                                 headers={"Content-Type": "application/json",
                                          "x-api-key": api_key(),
                                          "authorization": "Bearer " + api_key(),
                                          "anthropic-version": "2023-06-01"})
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            return resp.status == 200
    except urllib.error.HTTPError:
        return False
    except Exception:
        return False


def arm_result_path(arm):
    if arm == "probe":
        return HERE / "probe_full_listing.json"
    return HERE / ("results_arm_%s.json" % arm)


def already_done(arm):
    """已经有成功结果就不用再跑（429 失败不算成功）。"""
    p = arm_result_path(arm)
    if not p.is_file():
        return False
    try:
        data = json.loads(p.read_text(encoding="utf-8"))
    except Exception:
        return False
    if arm == "probe":
        return data.get("结果") in ("通过", "被拒")
    r = data["results"][0]
    if not r.get("ok") or r.get("error"):
        return False
    return True


def run_arm(arm):
    if already_done(arm):
        print("\n########## 实验臂 %s 已有成功结果，跳过 ##########" % arm, flush=True)
        return
    print("\n########## 补跑实验臂 %s ##########" % arm, flush=True)
    if arm == "probe":
        cmd = [sys.executable, "-u", str(HERE / "probe_full_listing.py")]
    else:
        cmd = [sys.executable, "-u", str(HERE / "retrieval_bench.py"),
               "--arm", arm,
               "--out", str(HERE / ("results_arm_%s.json" % arm)),
               "--answers", str(HERE / ("answers_arm_%s.md" % arm))]
    try:
        proc = subprocess.run(cmd, cwd=str(HERE.parent), capture_output=True, text=True,
                              encoding="utf-8", errors="replace", timeout=1800)
    except subprocess.TimeoutExpired:
        print("  !! 超时", flush=True)
        return
    for line in (proc.stdout or "").strip().splitlines()[-6:]:
        print("  " + line, flush=True)
    if proc.returncode != 0:
        print("  !! 退出码 %s" % proc.returncode, flush=True)
        for line in (proc.stderr or "").strip().splitlines()[-4:]:
            print("  ERR " + line, flush=True)
    p = arm_result_path(arm)
    if p.is_file():
        if arm == "probe":
            d = json.loads(p.read_text(encoding="utf-8"))
            print("  => 探针结果: %s (字符数 %s)" % (d.get("结果"), d.get("字符数")), flush=True)
        else:
            r = json.loads(p.read_text(encoding="utf-8"))["results"][0]
            hit = sum(s["命中"] for s in r.get("scores", {}).values())
            full = sum(s["总分"] for s in r.get("scores", {}).values())
            print("  => 得分 %d/%d %s" % (hit, full,
                                         "" if r.get("ok") else ("错误: " + str(r.get("error"))[:120])), flush=True)


def main():
    arms = sys.argv[1:] or QUEUE
    waited = 0
    while waited < MAX_WAIT_SECONDS and not quota_ok():
        print("[%s] 额度未恢复，%d 秒后再探…" % (time.strftime("%H:%M:%S"), POLL_SECONDS), flush=True)
        time.sleep(POLL_SECONDS)
        waited += POLL_SECONDS
    if waited >= MAX_WAIT_SECONDS:
        print("[%s] 本轮等待窗口用尽（额度仍未恢复），退出；稍后重跑本脚本即可断点续跑"
              % time.strftime("%H:%M:%S"), flush=True)
        return 2
    print("[%s] 额度已恢复，开始补跑 %s" % (time.strftime("%H:%M:%S"), arms), flush=True)
    for arm in arms:
        run_arm(arm)
    print("\n==== 补跑结束 ====", flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
