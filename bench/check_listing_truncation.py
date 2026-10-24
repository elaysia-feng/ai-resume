# -*- coding: utf-8 -*-
"""校验当前 harness 的清单裁剪策略能不能装下两题的答案文件（纯本地，不调模型）。

背景：results_b.json 那轮用的是 depth=8 的清单（1129 条 / 54148 字符），答案文件根本不在树里。
当前 retrieval_bench.file_listing 已是 depth=30，但 arm2 会再按 CTX_LIMIT_TOKENS 裁字符。
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from retrieval_bench import WORKSPACE, file_listing, CTX_LIMIT_TOKENS

ANSWER_FILES = {
    "Q1 TEMU店铺授权": "whp-temu/temu.web/src/main/java/com/erp/whp/temu/web/auth/TemuShopAuthInfoController.java",
    "Q2 1688授权": "whp-purchase/purchase.web/src/main/java/com/erp/whp/purchase/web/ocean1688/auth/Ocean1688AuthController.java",
}


def main():
    full = file_listing(WORKSPACE)
    budget_chars = CTX_LIMIT_TOKENS * 3 - 20000
    listing = full[:budget_chars]
    lines = listing.splitlines()
    out = {
        "完整清单": {"条目数": len(full.splitlines()), "字符数": len(full)},
        "裁剪预算字符": budget_chars,
        "裁剪后": {"条目数": len(lines), "字符数": len(listing)},
        "答案文件是否在裁剪后的树里": {},
        "答案目录是否在裁剪后的树里": {},
        "每轮重发字符数": len(listing),
    }
    for qname, rel in ANSWER_FILES.items():
        out["答案文件是否在裁剪后的树里"][qname] = rel in lines
        out["答案目录是否在裁剪后的树里"][qname] = (
            str(Path(rel).parent).replace("\\", "/") + "/" in lines)
    print(json.dumps(out, ensure_ascii=False, indent=2))
    Path("bench/listing_truncation_check.json").write_text(
        json.dumps(out, ensure_ascii=False, indent=2), encoding="utf-8")


if __name__ == "__main__":
    main()
