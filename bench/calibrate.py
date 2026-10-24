# -*- coding: utf-8 -*-
"""钉死两个关键数字（纯本地，不调模型）：

1) 要在路径树里装下「答案所在模块 whp-temu / whp-purchase」的任何文件，
   至少得带上树的前缀有多长 —— 这决定方案 2 平铺全树到底行不行。
2) 1b 实发的那 8,386,954 字节真实内容，对应多少字符、token 密度是多少，
   用来校准「上下文上限到底是多少 token」。
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from retrieval_bench import WORKSPACE, file_listing, walk_files, is_texty, read_text

OUT = {}


def prefix_to_module(targets):
    """树是深度优先 + 字母序。算出「覆盖到某模块最后一个文件」所需的前缀长度。"""
    lines = file_listing(WORKSPACE).splitlines()
    out = {}
    for mod in targets:
        idx = [i for i, l in enumerate(lines) if l.split("/")[0] == mod]
        if not idx:
            out[mod] = {"存在": False}
            continue
        end = idx[-1]
        prefix = lines[:end + 1]
        out[mod] = {
            "模块内条目数": len(idx),
            "需要前缀条目数": end + 1,
            "占完整树比例": round((end + 1) / len(lines), 3),
            "需要前缀字符数": sum(len(l) + 1 for l in prefix),
            "需要前缀估算tokens": sum(len(l) + 1 for l in prefix) // 3,
        }
    out["完整树"] = {"条目数": len(lines), "字符数": len(file_listing(WORKSPACE))}
    return out


def real_content_density():
    """1b 实发 8,386,954 字节那一份，算出它的字符数。"""
    files = [p for p in walk_files(WORKSPACE) if is_texty(p)]
    cap = 8 * 1024 * 1024
    sent_bytes = 0
    sent_chars = 0
    n = 0
    for p in files:
        text = read_text(p, max_bytes=200000)
        b = len(text.encode("utf-8"))
        if sent_bytes + b > cap:
            break
        sent_bytes += b
        sent_chars += len(text)
        n += 1
    return {
        "文件数": n,
        "字节": sent_bytes,
        "字符数": sent_chars,
        "字节每字符": round(sent_bytes / max(1, sent_chars), 2),
        "字符每token_按3估算": round(sent_chars / 3),
        "全量字符数": sum(len(read_text(p, max_bytes=200000)) for p in files),
    }


def main():
    OUT["A_方案2_装下答案模块所需前缀"] = prefix_to_module(["whp-temu", "whp-purchase"])
    OUT["B_真实内容token密度"] = real_content_density()
    print(json.dumps(OUT, ensure_ascii=False, indent=2))
    Path("bench/calibration.json").write_text(
        json.dumps(OUT, ensure_ascii=False, indent=2), encoding="utf-8")


if __name__ == "__main__":
    main()
