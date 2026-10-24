# -*- coding: utf-8 -*-
"""检查渲染出的 HTML：mermaid 块数量、是否还有残留围栏、表格是否成型。"""
import re
from pathlib import Path

p = Path("docs/取文件方式-流程对比.html")
t = p.read_text(encoding="utf-8")
print("mermaid div 数:", t.count('class="mermaid"'))
print("残留 ``` 数:", t.count("```"))
print("table 数:", t.count("<table>"))
print("h2 数:", t.count("<h2>"))
for m in re.finditer(r'<div class="mermaid">\n(.*?)\n</div>', t, re.S):
    first = m.group(1).split("\n")[0]
    print("  -", first[:60])
