# -*- coding: utf-8 -*-
"""按顶层模块统计文件树体积：判断「先给模块列表、再按模块下发子树」这条路够不够快。"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from retrieval_bench import WORKSPACE, file_listing

ANSWER_MODULE = {"Q1 TEMU店铺授权": "whp-temu", "Q2 1688授权": "whp-purchase"}


def main():
    full = file_listing(WORKSPACE).splitlines()
    per_mod = {}
    for l in full:
        top = l.split("/")[0]
        per_mod.setdefault(top, []).append(l)
    rows = []
    for top, entries in per_mod.items():
        text = "\n".join(entries)
        rows.append({
            "模块": top,
            "条目数": len(entries),
            "字符数": len(text),
            "估算tokens": len(text) // 3,
        })
    rows.sort(key=lambda r: -r["条目数"])
    out = {"按模块体积": rows, "答案模块": {}}
    for q, mod in ANSWER_MODULE.items():
        hit = [r for r in rows if r["模块"] == mod]
        out["答案模块"][q] = {"模块": mod, **(hit[0] if hit else {})}
    # 两级方案的总成本：模块清单 + 选中的模块子树
    mod_list = "\n".join("%s/ (%d 项)" % (r["模块"], r["条目数"]) for r in rows)
    out["第一级_模块清单字符数"] = len(mod_list)
    out["第二级_选中模块子树字符数"] = {r["模块"]: r["字符数"] for r in rows}
    out["结论"] = ("模块清单 %d 字符，模型点名单个模块后下发该模块子树，"
                   "单轮最坏 %d 字符（whp-goods %d）"
                   % (len(mod_list), max(r["字符数"] for r in rows),
                      max(rows, key=lambda r: r["字符数"])["条目数"]))
    print(json.dumps(out, ensure_ascii=False, indent=2))
    Path("bench/per_module_size.json").write_text(
        json.dumps(out, ensure_ascii=False, indent=2), encoding="utf-8")


if __name__ == "__main__":
    main()
