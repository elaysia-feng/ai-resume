# -*- coding: utf-8 -*-
"""纯本地统计：各实验臂要塞给模型的内容到底有多大。不调模型，不烧额度。"""
import json
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from retrieval_bench import (WORKSPACE, READABLE_SUFFIXES, SKIP_DIRS, file_listing,
                              walk_files, is_texty)

OUT = {}


def main():
    # 1) 完整文件清单（前端能给的最强形态）
    listing = file_listing(WORKSPACE)
    lines = listing.splitlines()
    OUT["完整文件清单"] = {
        "条目数": len(lines),
        "字符数": len(listing),
        "估算tokens": len(listing) // 3,
    }

    # 2) 工作区文本总量
    files = [p for p in walk_files(WORKSPACE) if is_texty(p)]
    total_bytes = 0
    for p in files:
        try:
            total_bytes += p.stat().st_size
        except OSError:
            pass
    OUT["工作区文本总量"] = {
        "文本文件数": len(files),
        "字节": total_bytes,
        "MB": round(total_bytes / 1024 / 1024, 1),
        "估算tokens": total_bytes // 3,
    }

    # 3) 按后缀分布
    by_suffix = {}
    for p in files:
        by_suffix[p.suffix.lower()] = by_suffix.get(p.suffix.lower(), 0) + 1
    OUT["按后缀文件数"] = dict(sorted(by_suffix.items(), key=lambda kv: -kv[1])[:12])

    # 4) 目录深度分布（前端树能省多少）
    depth_hist = {}
    for line in lines:
        if line.endswith("/"):
            continue
        d = line.count("/")
        depth_hist[d] = depth_hist.get(d, 0) + 1
    OUT["文件路径深度分布"] = dict(sorted(depth_hist.items()))
    OUT["深度<=8的文件数"] = sum(v for k, v in depth_hist.items() if k <= 8)
    OUT["深度>8的文件数"] = sum(v for k, v in depth_hist.items() if k > 8)

    # 5) 上下文上限对比
    limit = 500_000
    for name, key in (("完整文件清单", "完整文件清单"), ("工作区文本总量", "工作区文本总量")):
        need = OUT[name]["估算tokens"]
        OUT[name]["是否超500K上下文"] = need > limit
        OUT[name]["超出倍数"] = round(need / limit, 1)

    print(json.dumps(OUT, ensure_ascii=False, indent=2))
    Path("bench/size_report.json").write_text(json.dumps(OUT, ensure_ascii=False, indent=2),
                                               encoding="utf-8")


if __name__ == "__main__":
    main()
