# -*- coding: utf-8 -*-
"""不调模型、不烧额度的补充量化：把方案 2 / 方案 4 的失败原因算成可复现的数字。

A) 方案 2 的「树深度 -> 条目数/token/能否暴露答案文件」曲线
B) 方案 4 的 BM25 排序分析：答案文件排在第几？top-k 里有没有？
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from retrieval_bench import (WORKSPACE, QUESTIONS, file_listing, build_bm25, bm25_search,
                              TOP_K)

# 两题的答案文件（相对工作区根）
ANSWER_FILES = {
    "Q1 TEMU店铺授权": "whp-temu/temu.web/src/main/java/com/erp/whp/temu/web/auth/TemuShopAuthInfoController.java",
    "Q2 1688授权": "whp-purchase/purchase.web/src/main/java/com/erp/whp/purchase/web/ocean1688/auth/Ocean1688AuthController.java",
}
CTX_LIMIT_TOKENS = 500_000

OUT = {"A_方案2_树深度曲线": [], "B_方案4_BM25排序": {}}


def depth_curve():
    rows = []
    for depth in range(4, 17):
        listing = file_listing(WORKSPACE, depth=depth)
        lines = listing.splitlines()
        chars = len(listing)
        tok = chars // 3
        row = {
            "深度": depth,
            "条目数": len(lines),
            "字符数": chars,
            "估算tokens": tok,
            "超500K上下文": tok > CTX_LIMIT_TOKENS,
        }
        for qname, rel in ANSWER_FILES.items():
            row[qname + "_答案在树里"] = rel in lines
            row[qname + "_答案目录在树里"] = str(Path(rel).parent).replace("\\", "/") + "/" in lines
        rows.append(row)
    return rows


def bm25_rank():
    print("building bm25 index ...", flush=True)
    files, tokens, idf, avg_len, corpus_bytes, n_chunks = build_bm25(WORKSPACE)
    print("indexed files=%d chunks=%d bytes=%d" % (len(files), n_chunks, corpus_bytes), flush=True)
    res = {}
    for boost in (False, True):
        key = "BM25+文件名加权" if boost else "纯BM25"
        res[key] = {"索引文件数": len(files), "分块数": n_chunks, "各题": {}}
        for q in QUESTIONS:
            hits = bm25_search(tokens, idf, avg_len, q["question"], top_k=200, boost_filename=boost)
            ranked = [tokens[i][0] for _, i in hits]
            target = ANSWER_FILES[q["name"]]
            # 找该答案文件所属 chunk 的最好排名
            rank = None
            for pos, rel in enumerate(ranked, start=1):
                if rel == target:
                    rank = pos
                    break
            top20 = ranked[:TOP_K]
            res[key]["各题"][q["name"]] = {
                "答案文件最好排名": rank,
                "答案文件进top20": target in top20,
                "答案文件进top50": target in ranked[:50],
                "top8命中文件": ranked[:8],
                "top20里与题目相关的文件数": sum(
                    1 for r in top20
                    if ("temu" in r.lower() and "auth" in r.lower())
                    or ("ocean1688" in r.lower() and "auth" in r.lower())
                    or "总结" in r or "readme" in r.lower()),
            }
    return res


def main():
    OUT["A_方案2_树深度曲线"] = depth_curve()
    OUT["B_方案4_BM25排序"] = bm25_rank()
    print(json.dumps(OUT, ensure_ascii=False, indent=2))
    Path("bench/retrieval_analysis.json").write_text(
        json.dumps(OUT, ensure_ascii=False, indent=2), encoding="utf-8")


if __name__ == "__main__":
    main()
