# -*- coding: utf-8 -*-
"""把含 ```mermaid 代码块的 Markdown 渲染成单个 HTML，方便直接打开看图。

用法: python bench/render_mermaid.py docs/取文件方式-流程对比.md docs/取文件方式-流程对比.html
"""
import html
import re
import sys
from pathlib import Path

TEMPLATE = """<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{title}</title>
<style>
  :root {{ color-scheme: light; }}
  body {{ font-family: "Microsoft YaHei", "PingFang SC", system-ui, sans-serif;
         margin: 0; padding: 32px 40px 80px; background: #fbfbfd; color: #1f2328; line-height: 1.7; }}
  h1 {{ font-size: 26px; margin: 0 0 24px; }}
  h2 {{ font-size: 19px; margin: 40px 0 12px; padding-bottom: 8px; border-bottom: 1px solid #e3e6ea; }}
  h3 {{ font-size: 15px; margin: 24px 0 8px; color: #57606a; }}
  .card {{ background: #fff; border: 1px solid #e3e6ea; border-radius: 10px;
           padding: 20px 24px; margin: 16px 0 24px; box-shadow: 0 1px 3px rgba(0,0,0,.04); }}
  .mermaid {{ background: #fff; text-align: center; overflow-x: auto; }}
  table {{ border-collapse: collapse; width: 100%; margin: 12px 0 20px; background: #fff; font-size: 14px; }}
  th, td {{ border: 1px solid #e3e6ea; padding: 8px 12px; text-align: left; vertical-align: top; }}
  th {{ background: #f2f4f7; font-weight: 600; }}
  code {{ background: #f2f4f7; padding: 1px 5px; border-radius: 4px; font-size: 13px; }}
  pre {{ background: #f6f8fa; padding: 14px 16px; border-radius: 8px; overflow-x: auto;
         font-size: 13px; line-height: 1.6; }}
  ul {{ padding-left: 22px; }}
  li {{ margin: 4px 0; }}
  blockquote {{ margin: 16px 0; padding: 8px 16px; border-left: 3px solid #d0d7de;
                background: #f6f8fa; color: #57606a; }}
  hr {{ border: none; border-top: 1px solid #e3e6ea; margin: 36px 0; }}
</style>
</head>
<body>
{body}
<script src="https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.min.js"></script>
<script>
  window.mermaid.initialize({{ startOnLoad: true, theme: 'default', securityLevel: 'loose' }});
</script>
</body>
</html>
"""


def main():
    src = Path(sys.argv[1])
    dst = Path(sys.argv[2])
    text = src.read_text(encoding="utf-8")

    # 1) 把 mermaid 代码块换成 <div class="mermaid">
    def repl(m):
        return '<div class="mermaid">\n%s\n</div>' % m.group(1)
    out = re.sub(r"```mermaid\n(.*?)```", repl, text, flags=re.S)

    # 2) 把普通代码块（非 mermaid）保留为 pre
    out = re.sub(r"```(\w*)\n(.*?)```",
                 lambda m: '<pre><code>%s</code></pre>' % html.escape(m.group(2)),
                 out, flags=re.S)

    # 3) 极简 Markdown -> HTML
    out = re.sub(r"^### (.+)$", r"<h3>\1</h3>", out, flags=re.M)
    out = re.sub(r"^## (.+)$", r"<h2>\1</h2>", out, flags=re.M)
    out = re.sub(r"^# (.+)$", r"<h1>\1</h1>", out, flags=re.M)
    out = re.sub(r"^> (.+)$", r"<blockquote>\1</blockquote>", out, flags=re.M)
    out = re.sub(r"^---$", "<hr>", out, flags=re.M)

    # 表格
    lines = out.split("\n")
    i = 0
    while i < len(lines):
        if lines[i].startswith("|") and i + 1 < len(lines) and re.match(r"^\|[\s\-:|]+\|$", lines[i + 1]):
            header = [c.strip() for c in lines[i].strip().strip("|").split("|")]
            start = i
            i += 2
            rows = []
            while i < len(lines) and lines[i].startswith("|"):
                rows.append([c.strip() for c in lines[i].strip().strip("|").split("|")])
                i += 1
            th = "".join("<th>%s</th>" % c for c in header)
            trs = "".join("<tr>%s</tr>" % "".join("<td>%s</td>" % c for c in r) for r in rows)
            lines[start:i] = ["<table><thead><tr>%s</tr></thead><tbody>%s</tbody></table>" % (th, trs)]
            continue
        i += 1
    out = "\n".join(lines)

    # 列表
    out = re.sub(r"^- (.+)$", r"<li>\1</li>", out, flags=re.M)
    out = re.sub(r"(<li>.*?</li>\n?)+", lambda m: "<ul>%s</ul>" % m.group(0).replace("\n", ""), out, flags=re.S)
    # 行内 code
    out = re.sub(r"`([^`]+)`", r"<code>\1</code>", out)
    # 粗体（先转 code 之外的 **xx**）
    out = re.sub(r"\*\*([^*\n]+)\*\*", r"<strong>\1</strong>", out)
    # 段落
    out = re.sub(r"\n{3,}", "\n\n", out)

    title = src.stem
    dst.write_text(TEMPLATE.format(title=html.escape(title), body=out), encoding="utf-8")
    print("已生成: %s (%d 字节)" % (dst, dst.stat().st_size))


if __name__ == "__main__":
    main()
