# -*- coding: utf-8 -*-
"""验证 arm 2b 的 ACTION 解析与答案提取（离线，不调模型）。"""
import re

P = r"ACTION:[ \t]*(read_dir|read_file|answer)[ \t]*([^\n\r]*)"

CASES = [
    "ACTION: answer\n**Controller 文件：**\n`whp-temu/x.java`",
    "ACTION: read_file whp-temu/temu.web/src/main/java/com/erp/whp/temu/web/auth/TemuShopAuthInfoController.java",
    "ACTION: read_dir whp-temu\n下一步",
    "**结论**\nACTION: answer",
    "ACTION: answer",
]


def extract(text):
    m = re.search(P, text)
    if not m:
        return None, None
    action, arg = m.group(1), (m.group(2) or "").strip().strip("`\"'")
    if action != "answer":
        return action, arg
    final = "\n".join(ln for ln in text.splitlines()
                      if not ln.strip().startswith("ACTION:")).strip() or text
    return action, final


for t in CASES:
    a, v = extract(t)
    print("action=%-9s value=%r" % (a, (v or "")[:70]))
