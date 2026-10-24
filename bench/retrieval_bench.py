# -*- coding: utf-8 -*-
"""取文件方式对比基准：同模型、同题目、同工作区，只变「文件内容怎么进 prompt」。

5 个实验臂：
  1a 前端直读「用户选中的文件」全文 -> 一次 prompt
  1b 前端直读整个工作区             -> 一次 prompt（实测会不会超限）
  2  前端只给文件清单 -> 模型点名要文件 -> 前端读 -> 回传 -> 多轮
  3  后端直读全量内容               -> 一次 prompt（实测会不会超限）
  4  后端一次性检索(BM25)           -> top-k 片段 + 清单 -> 一次 prompt
  5  后端 tool calling              -> 模型自己决定读什么（现有 Java 实现的做法）

评分是确定性的：每题 5 个采分点，答案里出现正确的东西才得分。
题目答案只存在于代码里，根目录那两份架构 md 不含答案（已核对）。

用法：
    python bench/retrieval_bench.py                # 跑全部实验臂
    python bench/retrieval_bench.py --arm 5        # 只跑某一臂
"""
import argparse
import json
import os
import re
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

API_URL = "https://api.minimaxi.com/anthropic/v1/messages"
MODEL = "MiniMax-M3.1-Flash"
# 统一给足输出预算：太小会把「模型在噪声里空转烧光 token」误记成策略失败
MAX_TOKENS = 8192
# 实测 MiniMax-M3.1-Flash 的上下文上限约 500K input tokens（8M 字符的请求能过）
CTX_LIMIT_TOKENS = 500_000
WORKSPACE = Path(r"C:\Users\seele\Desktop\whp-multiplatform-master\whp-multiplatform-master")

# ---------------------------------------------------------------- 题目与标准答案
# 标准答案直接从代码里核出来，评分不靠人肉感觉
QUESTIONS = [
    {
        "name": "Q1 TEMU店铺授权",
        "question": "whp-temu 模块里，平台店铺授权信息（ShopAuthInfo）的 Controller 是哪个文件？"
                    "请给出该文件在工作区中的相对路径，并列出它对外提供的接口路径。",
        "points": [
            ("类名", lambda a: "TemuShopAuthInfoController" in a),
            ("文件路径", lambda a: "whp-temu" in a and ("TemuShopAuthInfoController.java" in a
                                                 or "temu/web/auth" in a or "temu\\web\\auth" in a)),
            ("类级路径", lambda a: "/temu/shop_auth" in a),
            ("方法saveOrUpdate", lambda a: "saveOrUpdateShopAuthInfo" in a),
            ("方法getByShopId", lambda a: "getByShopId" in a),
        ],
    },
    {
        "name": "Q2 1688授权",
        "question": "whp-purchase 模块里，对接 1688 平台授权的 Controller 是哪个文件？"
                    "请给出该文件在工作区中的相对路径，并列出它对外提供的接口路径。",
        "points": [
            ("类名", lambda a: "Ocean1688AuthController" in a),
            ("文件路径", lambda a: "whp-purchase" in a and ("Ocean1688AuthController.java" in a
                                                    or "ocean1688/auth" in a or "ocean1688\\auth" in a)),
            ("类级路径", lambda a: "/ocean1688/auth" in a),
            ("方法getAuthUrl", lambda a: "getAuthUrl" in a),
            ("方法callback", lambda a: "/callback" in a and "1688" in a),
        ],
    },
]

SYSTEM_PROMPT = (
    "你是一个代码分析助手，回答必须以工作区里的真实文件为依据。"
    "拿不到依据就明说「没找到」，不要编造文件路径、类名或接口路径。"
    "回答用简体中文，尽量简洁。"
)

READABLE_SUFFIXES = {".java", ".kt", ".md", ".yml", ".yaml", ".xml", ".json", ".properties",
                     ".sql", ".sh", ".txt", ".html", ".css", ".js", ".ts", ".vue", ".gradle"}
SKIP_DIRS = {".git", ".idea", "node_modules", "target", "build", "dist", ".gradle", ".omc",
             ".workbuddy", "logfile"}


# ---------------------------------------------------------------- LLM 调用

def api_key():
    settings = Path(os.path.expanduser("~")) / ".claude" / "settings.json"
    data = json.loads(settings.read_text(encoding="utf-8"))
    return (data.get("env") or {}).get("ANTHROPIC_AUTH_TOKEN", "").strip()


def call_llm(messages, tools=None, max_tokens=MAX_TOKENS):
    body = {"model": MODEL, "max_tokens": max_tokens, "system": SYSTEM_PROMPT, "messages": messages}
    if tools:
        body["tools"] = tools
    req = urllib.request.Request(API_URL, data=json.dumps(body).encode("utf-8"), method="POST", headers={
        "Content-Type": "application/json",
        "x-api-key": api_key(),
        "authorization": "Bearer " + api_key(),
        "anthropic-version": "2023-06-01",
    })
    started = time.time()
    try:
        with urllib.request.urlopen(req, timeout=900) as resp:
            payload = json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        raise RuntimeError("HTTP %d: %s" % (e.code, e.read().decode("utf-8", "replace")[:400]))
    elapsed = time.time() - started
    text = "".join(b.get("text", "") for b in payload.get("content", []) if b.get("type") == "text")
    tool_uses = [b for b in payload.get("content", []) if b.get("type") == "tool_use"]
    usage = payload.get("usage") or {}
    return {"text": text, "tool_uses": tool_uses, "stop_reason": payload.get("stop_reason"),
            "input_tokens": usage.get("input_tokens", 0), "output_tokens": usage.get("output_tokens", 0),
            "elapsed": elapsed}


# ---------------------------------------------------------------- 文件访问原语

def walk_files(root, max_files=400000):
    out = []
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
        for name in filenames:
            out.append(Path(dirpath) / name)
            if len(out) >= max_files:
                return out
    return out


def is_texty(path):
    return path.suffix.lower() in READABLE_SUFFIXES


def read_text(path, max_bytes=200000):
    try:
        data = path.read_bytes()
    except OSError:
        return ""
    if b"\x00" in data[:4096]:
        return ""
    return data[:max_bytes].decode("utf-8", "replace")


def file_listing(root, max_entries=200000, depth=30):
    """前端能给的「文件清单」：只有相对路径，没有内容。"""
    lines = []

    def walk(dir_path, level):
        if level > depth or len(lines) >= max_entries:
            return
        try:
            entries = sorted(os.scandir(dir_path), key=lambda e: e.name)
        except OSError:
            return
        for e in entries:
            if len(lines) >= max_entries:
                return
            if e.name in SKIP_DIRS or e.name.startswith("."):
                continue
            rel = Path(e.path).relative_to(root).as_posix()
            if e.is_dir():
                lines.append(rel + "/")
                walk(e.path, level + 1)
            else:
                lines.append(rel)

    walk(root, 0)
    return "\n".join(lines)


# ---------------------------------------------------------------- 结果容器

class ArmResult:
    def __init__(self, name):
        self.name = name
        self.ok = False
        self.error = ""
        self.llm_calls = 0
        self.reads = 0
        self.read_bytes = 0
        self.input_tokens = 0
        self.output_tokens = 0
        self.elapsed = 0.0
        self.answers = {}
        self.scores = {}
        self.extra = {}

    def to_dict(self):
        return dict(self.__dict__)


def score_answer(answer, q):
    hits = [(label, bool(fn(answer))) for label, fn in q["points"]]
    return {"命中": sum(1 for _, ok in hits if ok), "总分": len(hits),
            "明细": {label: ("√" if ok else "×") for label, ok in hits}}


# ---------------------------------------------------------------- 实验臂

def arm1a_frontend_read_picked(root, answer):
    """前端直读「用户已经选中的文件」全文。oracle：用户已知道该选哪两个文件。"""
    res = ArmResult("1a 前端直读选中文件(用户已知答案文件)")
    picked = [
        root / "whp-temu/temu.web/src/main/java/com/erp/whp/temu/web/auth/TemuShopAuthInfoController.java",
        root / "whp-purchase/purchase.web/src/main/java/com/erp/whp/purchase/web/ocean1688/auth/Ocean1688AuthController.java",
    ]
    for q in QUESTIONS:
        parts = []
        for p in picked:
            text = read_text(p)
            res.reads += 1
            res.read_bytes += len(text.encode("utf-8"))
            parts.append("===== 文件: %s =====\n%s" % (p.relative_to(root).as_posix(), text))
        ctx = "\n\n".join(parts)
        messages = [{"role": "user", "content": ctx + "\n\n---\n问题：" + q["question"]}]
        resp = call_llm(messages)
        res.llm_calls += 1
        res.input_tokens += resp["input_tokens"]
        res.output_tokens += resp["output_tokens"]
        res.elapsed += resp["elapsed"]
        res.answers[q["name"]] = resp["text"]
        answer[q["name"]] = resp["text"]
    res.ok = True
    return res


def _bulk_read(root, res, cap_bytes=8 * 1024 * 1024):
    """把工作区里所有文本文件读进来，统计真实体积；超过 cap 就截断并记录。"""
    files = [p for p in walk_files(root) if is_texty(p)]
    total_all = 0
    for p in files:
        try:
            total_all += p.stat().st_size
        except OSError:
            pass
    res.extra["文本文件数"] = len(files)
    res.extra["全量字节(所有文本文件)"] = total_all
    res.extra["全量估算tokens"] = total_all // 3
    blob = []
    sent = 0
    for p in files:
        text = read_text(p, max_bytes=200000)
        b = len(text.encode("utf-8"))
        if sent + b > cap_bytes:
            break
        sent += b
        blob.append("===== %s =====\n%s" % (p.relative_to(root).as_posix(), text))
    res.extra["实际送出的字节"] = sent
    res.extra["实际送出的估算tokens"] = sent // 3
    return blob, total_all


def arm1b_frontend_read_all(root, answer):
    """前端直读整个工作区：量真实体积，再真发一次看服务端怎么拒。"""
    res = ArmResult("1b 前端直读整个工作区")
    blob, total = _bulk_read(root, res)
    for q in QUESTIONS:
        try:
            messages = [{"role": "user", "content": "\n\n".join(blob) + "\n\n---\n问题：" + q["question"]}]
            resp = call_llm(messages)
            res.llm_calls += 1
            res.input_tokens += resp["input_tokens"]
            res.output_tokens += resp["output_tokens"]
            res.elapsed += resp["elapsed"]
            res.answers[q["name"]] = resp["text"]
            answer[q["name"]] = resp["text"]
        except RuntimeError as e:
            res.error = str(e)
            break
    return res


def arm2_frontend_tree_loop(root, answer):
    """前端只给文件清单；模型用自然语言点名要文件，前端读回来再喂。多轮。

    先用「完整清单」实测一次（前端能给的最强形态），再退到「装得下的最大裁剪清单」跑多轮。
    """
    res = ArmResult("2 前端只给文件清单(自然语言点名,多轮)")
    full = file_listing(root)
    full_lines = full.splitlines()
    res.extra["完整清单条目数"] = len(full_lines)
    res.extra["完整清单字符数"] = len(full)
    res.extra["完整清单估算tokens"] = len(full) // 3
    res.extra["是否超上下文"] = len(full) // 3 > CTX_LIMIT_TOKENS

    # 第一步：完整清单真发一次
    if len(full) // 3 > CTX_LIMIT_TOKENS:
        try:
            messages = [{"role": "user", "content": full + "\n\n问题：测试"}]
            call_llm(messages, max_tokens=16)
            res.extra["完整清单请求"] = "居然发出去了"
        except RuntimeError as e:
            res.extra["完整清单请求"] = "失败: " + str(e)[:200]
    else:
        res.extra["完整清单请求"] = "发出去了"

    # 第二步：裁剪到能装下，跑多轮协议
    budget_chars = CTX_LIMIT_TOKENS * 3 - 20000
    listing = full[:budget_chars]
    res.extra["实际使用清单条目数"] = len(listing.splitlines())
    hint = ("你可以要求读取文件，格式固定为一行：读取文件: <相对路径>\n"
            "我读到你指定的文件后会把内容贴给你，然后你再回答。")
    for q in QUESTIONS:
        messages = [{"role": "user", "content":
                      "工作区文件清单（只有路径，没有内容）：\n%s\n\n%s\n\n---\n问题：%s"
                      % (listing, hint, q["question"])}]
        final = ""
        parse_fail = 0
        for _ in range(10):
            resp = call_llm(messages)
            res.llm_calls += 1
            res.input_tokens += resp["input_tokens"]
            res.output_tokens += resp["output_tokens"]
            res.elapsed += resp["elapsed"]
            if resp["text"].strip():
                final = resp["text"]
            m = re.search(r"读取文件[:：]\s*(\S+)", resp["text"])
            if not m:
                break
            target = (root / m.group(1).strip().strip("`\"'")).resolve()
            try:
                target.relative_to(root.resolve())
            except ValueError:
                parse_fail += 1
                messages.append({"role": "assistant", "content": resp["text"]})
                messages.append({"role": "user", "content": "该路径不在工作区内，换一个。"})
                continue
            if not target.is_file():
                parse_fail += 1
                messages.append({"role": "assistant", "content": resp["text"]})
                messages.append({"role": "user", "content": "文件不存在：%s" % m.group(1)})
                continue
            text = read_text(target, max_bytes=120000)
            res.reads += 1
            res.read_bytes += len(text.encode("utf-8"))
            messages.append({"role": "assistant", "content": resp["text"]})
            messages.append({"role": "user", "content": "===== 文件: %s =====\n%s" % (m.group(1), text)})
        res.extra.setdefault("解析失败次数", {})[q["name"]] = parse_fail
        res.answers[q["name"]] = final
        answer[q["name"]] = final
    res.ok = True
    return res


# ---- 实验臂 2b：方案 2 的两级树变体（先模块清单，再按模块下发子树）

def module_index(root):
    """第一级：顶层模块名 + 条目数。这是前端能给出的最小可导航结构。"""
    listing = file_listing(root).splitlines()
    per_mod = {}
    for line in listing:
        per_mod.setdefault(line.split("/")[0], []).append(line)
    rows = []
    for mod, entries in sorted(per_mod.items()):
        rows.append((mod, len(entries)))
    return listing, per_mod, rows


def arm2b_two_level_tree(root, answer):
    """方案 2 的可行形态：第 1 轮给模块清单，模型点名模块后第 2 轮给该模块子树，
    模型再点名文件，前端读回内容。多轮，直到模型给出最终答案。

    协议：模型每轮必须输出一行 ACTION:
      ACTION: read_dir <模块名>
      ACTION: read_file <工作区相对路径>
      ACTION: answer
    """
    res = ArmResult("2b 前端只给文件树(两级: 模块清单 -> 模块子树)")
    listing, per_mod, rows = module_index(root)
    index_text = "\n".join("%s/  (%d 个条目)" % (m, n) for m, n in rows)
    res.extra["第一级模块清单字符数"] = len(index_text)
    res.extra["第一级模块数"] = len(rows)
    res.extra.setdefault("下发模块", {})

    hint = (
        "你在通过一个只能看路径、看不到内容的文件树探索工作区。\n"
        "规则：\n"
        "1) 如果你还需要更多信息，就在回复里写一行 ACTION: 开头的指令：\n"
        "     ACTION: read_dir <模块名>   —— 索取该模块下的完整路径列表\n"
        "     ACTION: read_file <路径>    —— 索取某个文件的内容（工作区相对路径）\n"
        "   同一轮只能写一行指令，等我回你内容后再继续。\n"
        "2) 如果你已经拿到足够依据，就**直接写最终答案**，不要再写 ACTION 行。\n"
        "   最终答案要包含：文件相对路径、类级接口路径、对外提供的方法。\n"
        "拿到依据前不要凭空猜测文件路径。")
    for q in QUESTIONS:
        messages = [{"role": "user", "content":
                      "工作区顶层模块清单：\n%s\n\n%s\n\n---\n问题：%s"
                      % (index_text, hint, q["question"])}]
        final = ""
        parse_fail = 0
        for _ in range(8):
            resp = call_llm(messages)
            res.llm_calls += 1
            res.input_tokens += resp["input_tokens"]
            res.output_tokens += resp["output_tokens"]
            res.elapsed += resp["elapsed"]
            text = resp["text"] or ""
            m = re.search(r"ACTION:[ \t]*(read_dir|read_file)[ \t]*([^\n\r]*)", text)
            if not m:
                # 没有 ACTION 行 => 模型认为依据已足够，这整条消息就是最终答案
                final = text.strip()
                messages.append({"role": "assistant", "content": text or "(空)"})
                break
            action = m.group(1)
            arg = (m.group(2) or "").strip().strip("`\"'")
            if action == "read_dir":
                entries = per_mod.get(arg)
                if not entries:
                    parse_fail += 1
                    messages.append({"role": "assistant", "content": text})
                    messages.append({"role": "user", "content": "没有这个模块：%s" % arg})
                    continue
                res.extra["下发模块"].setdefault(q["name"], []).append(arg)
                payload = "模块 %s 下的路径列表（%d 条）：\n%s" % (arg, len(entries), "\n".join(entries))
                messages.append({"role": "assistant", "content": text})
                messages.append({"role": "user", "content": payload})
                continue
            # read_file
            target = (root / arg).resolve()
            try:
                target.relative_to(root.resolve())
            except ValueError:
                parse_fail += 1
                messages.append({"role": "assistant", "content": text})
                messages.append({"role": "user", "content": "该路径不在工作区内，换一个。"})
                continue
            if not target.is_file():
                parse_fail += 1
                messages.append({"role": "assistant", "content": text})
                messages.append({"role": "user", "content": "文件不存在：%s" % arg})
                continue
            body = read_text(target, max_bytes=120000)
            res.reads += 1
            res.read_bytes += len(body.encode("utf-8"))
            messages.append({"role": "assistant", "content": text})
            messages.append({"role": "user", "content": "===== 文件: %s =====\n%s" % (arg, body)})
        if not final:
            final = (messages[-1].get("content") or "") if messages else ""
        res.extra.setdefault("解析失败次数", {})[q["name"]] = parse_fail
        res.answers[q["name"]] = final
        answer[q["name"]] = final
    res.ok = True
    return res


def arm3_backend_full_dump(root, answer):
    """后端直读全量内容，一次性给模型。"""
    res = ArmResult("3 后端直读全量")
    blob, total = _bulk_read(root, res)
    for q in QUESTIONS:
        try:
            messages = [{"role": "user", "content": "\n\n".join(blob) + "\n\n---\n问题：" + q["question"]}]
            resp = call_llm(messages)
            res.llm_calls += 1
            res.input_tokens += resp["input_tokens"]
            res.output_tokens += resp["output_tokens"]
            res.elapsed += resp["elapsed"]
            res.answers[q["name"]] = resp["text"]
            answer[q["name"]] = resp["text"]
        except RuntimeError as e:
            res.error = str(e)
            break
    return res


# ---- 实验臂 4：后端一次性检索（BM25）

CHUNK_SIZE = 1200
CHUNK_OVERLAP = 200
TOP_K = 20


def build_bm25(root, max_bytes=40 * 1024 * 1024):
    """建 BM25 倒排。

    内存敏感：4.5 万个 chunk 若按「token 列表」存，会有近 50 万个小字符串对象，
    光索引就吃 500MB 以上，机器内存紧张时会被 OOM 直接杀掉。
    这里改成每个 chunk 存一条「空格连接的分词串」+ token 数，
    内存降到 1/10，打分时再 split 一次。
    """
    import math
    from collections import Counter

    files = [p for p in walk_files(root) if is_texty(p)]
    docs = []          # [(相对路径, 原文片段, "tok1 tok2 ...", token 数)]
    df = Counter()
    total = 0
    n_chunks = 0
    step = CHUNK_SIZE - CHUNK_OVERLAP
    for p in files:
        text = read_text(p, max_bytes=200000)
        if not text.strip():
            continue
        b = len(text.encode("utf-8"))
        if total + b > max_bytes:
            break
        total += b
        rel = p.relative_to(root).as_posix()
        for i in range(0, len(text), step):
            piece = text[i:i + CHUNK_SIZE]
            if len(piece.strip()) < 40:
                continue
            toks = re.findall(r"[A-Za-z_][A-Za-z0-9_]{2,}|[0-9]{2,}", piece.lower())
            if not toks:
                continue
            docs.append((rel, piece, " ".join(toks), len(toks)))
            df.update(set(toks))
            n_chunks += 1
    n = max(1, len(docs))
    idf = {t: math.log(1 + (n - c + 0.5) / (c + 0.5)) for t, c in df.items()}
    avg_len = sum(d[3] for d in docs) / n if docs else 1
    return files, docs, idf, avg_len, total, n_chunks


def bm25_search(tokens, idf, avg_len, query, top_k=TOP_K, boost_filename=False):
    from collections import Counter
    q = re.findall(r"[A-Za-z_][A-Za-z0-9_]{2,}|[0-9]{2,}", query.lower())
    scores = []
    for idx, (rel, _piece, toks_str, tlen) in enumerate(tokens):
        tf = Counter(toks_str.split())
        s = 0.0
        for t in q:
            f = tf.get(t)
            if not f:
                continue
            s += idf.get(t, 0) * (f * 2.5) / (f + 1.5 * (0.25 + 0.75 * tlen / avg_len))
        if boost_filename:
            # 文件名/路径里出现查询词就大幅加权（符号名往往就在文件名里）
            low_rel = rel.lower()
            for t in q:
                if t in low_rel:
                    s += idf.get(t, 0) * 8.0
        if s > 0:
            scores.append((s, idx))
    scores.sort(reverse=True)
    return scores[:top_k]


def arm4_backend_rag(root, answer):
    return _arm4(root, answer, boost_filename=False, name="4 后端一次性检索(BM25 top-k)")


def arm4b_backend_rag_boosted(root, answer):
    return _arm4(root, answer, boost_filename=True, name="4b 后端检索(BM25+文件名加权)")


def _arm4(root, answer, boost_filename, name):
    res = ArmResult(name)
    t0 = time.time()
    files, tokens, idf, avg_len, corpus_bytes, n_chunks = build_bm25(root)
    res.extra["索引文件数"] = len(files)
    res.extra["索引字节数"] = corpus_bytes
    res.extra["分块数"] = n_chunks
    res.extra["建索引秒数"] = round(time.time() - t0, 1)
    for q in QUESTIONS:
        hits = bm25_search(tokens, idf, avg_len, q["question"], boost_filename=boost_filename)
        picked = []
        for score, idx in hits:
            rel, piece = tokens[idx][0], tokens[idx][1]
            picked.append("===== %s (score=%.2f) =====\n%s" % (rel, score, piece))
        res.extra.setdefault("命中文件", {})[q["name"]] = [tokens[i][0] for _, i in hits[:8]]
        res.reads += len(picked)
        res.read_bytes += sum(len(p.encode("utf-8")) for p in picked)
        messages = [{"role": "user", "content": "\n\n".join(picked) + "\n\n---\n问题：" + q["question"]}]
        resp = call_llm(messages)
        res.llm_calls += 1
        res.input_tokens += resp["input_tokens"]
        res.output_tokens += resp["output_tokens"]
        res.elapsed += resp["elapsed"]
        res.answers[q["name"]] = resp["text"]
        answer[q["name"]] = resp["text"]
    res.ok = True
    return res


# ---- 实验臂 5：后端 tool calling（现有 Java 实现的做法）

TOOLS = [
    {"name": "list_dir", "description": "列出工作区某个目录下的文件和子目录。只返回路径，不返回内容。",
     "input_schema": {"type": "object", "properties": {
         "path": {"type": "string", "description": "相对工作区根目录的路径，空表示根目录"}},
         "required": ["path"]}},
    {"name": "read_local_file", "description": "读取工作区内某个文本文件的内容。",
     "input_schema": {"type": "object", "properties": {
         "path": {"type": "string", "description": "相对工作区根目录的文件路径"},
         "maxBytes": {"type": "integer", "description": "最多读取多少字节，默认 200000"}},
         "required": ["path"]}},
    {"name": "search_in_files", "description": "在工作区内按内容搜索，返回命中的文件路径和行。",
     "input_schema": {"type": "object", "properties": {
         "query": {"type": "string"},
         "path": {"type": "string", "description": "限定目录，可空"}},
         "required": ["query"]}},
]


def tool_call(root, name, args, res):
    path_arg = (args.get("path") or "").strip()
    if name == "list_dir":
        target = (root / path_arg) if path_arg else root
        try:
            target.resolve().relative_to(root.resolve())
        except ValueError:
            return "路径越界"
        if not target.is_dir():
            return "目录不存在"
        names = []
        for e in sorted(os.scandir(target), key=lambda x: x.name):
            if e.name in SKIP_DIRS:
                continue
            names.append(e.name + ("/" if e.is_dir() else ""))
            if len(names) >= 200:
                break
        return "\n".join(names)
    if name == "read_local_file":
        target = (root / path_arg).resolve()
        try:
            target.relative_to(root.resolve())
        except ValueError:
            return "路径越界"
        if not target.is_file():
            return "文件不存在"
        text = read_text(target, max_bytes=int(args.get("maxBytes") or 200000))
        res.reads += 1
        res.read_bytes += len(text.encode("utf-8"))
        return "===== %s =====\n%s" % (path_arg, text)
    if name == "search_in_files":
        query = (args.get("query") or "").strip()
        if not query:
            return "query 不能为空"
        hits = []
        base = (root / path_arg) if path_arg else root
        for p in walk_files(base):
            if not is_texty(p):
                continue
            text = read_text(p, max_bytes=60000)
            for i, line in enumerate(text.splitlines(), 1):
                if query.lower() in line.lower():
                    hits.append("%s:%d: %s" % (p.relative_to(root).as_posix(), i, line.strip()[:160]))
                    if len(hits) >= 60:
                        return "\n".join(hits)
        return "\n".join(hits) if hits else "没有命中"
    return "未知工具"


def arm5_tool_calling(root, answer):
    res = ArmResult("5 后端 tool calling(模型自主读)")
    for q in QUESTIONS:
        messages = [{"role": "user", "content": q["question"]}]
        final = ""
        for _ in range(12):
            resp = call_llm(messages, tools=TOOLS)
            res.llm_calls += 1
            res.input_tokens += resp["input_tokens"]
            res.output_tokens += resp["output_tokens"]
            res.elapsed += resp["elapsed"]
            if resp["text"].strip():
                final = resp["text"]
            if not resp["tool_uses"]:
                break
            messages.append({"role": "assistant", "content": resp["text"] or "(调用工具)"})
            for tu in resp["tool_uses"]:
                out = tool_call(root, tu["name"], tu.get("input") or {}, res)
                messages.append({"role": "user", "content":
                                 "<tool_result name=%s id=%s>\n%s\n</tool_result>" % (tu["name"], tu["id"], out)})
        res.answers[q["name"]] = final
        answer[q["name"]] = final
    res.ok = True
    return res


ARMS = {
    "1a": arm1a_frontend_read_picked,
    "1b": arm1b_frontend_read_all,
    "2": arm2_frontend_tree_loop,
    "2b": arm2b_two_level_tree,
    "3": arm3_backend_full_dump,
    "4": arm4_backend_rag,
    "4b": arm4b_backend_rag_boosted,
    "5": arm5_tool_calling,
}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--arm", default="all")
    parser.add_argument("--out", default="bench/results.json")
    parser.add_argument("--answers", default="bench/answers.md")
    args = parser.parse_args()

    if not WORKSPACE.is_dir():
        print("工作区不存在：%s" % WORKSPACE)
        return 2

    selected = list(ARMS) if args.arm == "all" else [a.strip() for a in args.arm.split(",")]
    all_results = []
    answers = {}
    for key in selected:
        print("\n########## 实验臂 %s ##########" % key, flush=True)
        started = time.time()
        try:
            res = ARMS[key](WORKSPACE, answers)
        except Exception as exc:  # noqa: BLE001
            res = ArmResult("%s(%s)" % (key, ARMS[key].__name__))
            res.error = "%s: %s" % (type(exc).__name__, exc)
        res.extra["墙钟秒数"] = round(time.time() - started, 1)
        for q in QUESTIONS:
            s = score_answer(res.answers.get(q["name"], ""), q)
            res.scores[q["name"]] = s
            print("  %s 得分 %d/%d %s" % (q["name"], s["命中"], s["总分"], s["明细"]), flush=True)
        print("  LLM调用 %d 次 / 读文件 %d 个 / %d 字节 / prompt %d tok / 输出 %d tok / %.1fs"
              % (res.llm_calls, res.reads, res.read_bytes, res.input_tokens,
                 res.output_tokens, res.elapsed), flush=True)
        if res.error:
            print("  失败: %s" % res.error[:300], flush=True)
        all_results.append(res)

    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps({"workspace": str(WORKSPACE), "model": MODEL,
                                "results": [r.to_dict() for r in all_results]},
                               ensure_ascii=False, indent=2), encoding="utf-8")

    ans_path = Path(args.answers)
    lines = ["# 各实验臂的原始回答", ""]
    for q in QUESTIONS:
        lines.append("## %s" % q["name"])
        lines.append("")
        lines.append("题目：%s" % q["question"])
        lines.append("")
        for r in all_results:
            lines.append("### %s" % r.name)
            lines.append("")
            lines.append("```")
            lines.append((r.answers.get(q["name"]) or "(无)").strip())
            lines.append("```")
            lines.append("")
    ans_path.write_text("\n".join(lines), encoding="utf-8")

    print("\n==== 汇总 ====")
    header = "实验臂 | 状态 | LLM调用 | 读文件 | 读字节 | prompt tok | 输出 tok | 秒 | 得分"
    print(header)
    for r in all_results:
        total = sum(s["命中"] for s in r.scores.values())
        full = sum(s["总分"] for s in r.scores.values())
        print("%-34s | %-4s | %d | %d | %d | %d | %d | %.1f | %d/%d" % (
            r.name, "OK" if r.ok else "失败", r.llm_calls, r.reads, r.read_bytes,
            r.input_tokens, r.output_tokens, r.elapsed, total, full))
    print("\n结果: %s / %s" % (out, ans_path))
    return 0


if __name__ == "__main__":
    sys.exit(main())
