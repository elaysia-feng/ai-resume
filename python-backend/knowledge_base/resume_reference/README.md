---
source: resume_reference/README.md
module: GENERAL
kind: import_guide
tags:
  - rag
  - resume
  - guide
---

# 简历 RAG 参考库

这个目录存放可导入向量库的 Markdown 知识片段，用于简历改写、JD 匹配和模块优化。

## 文件组织

- `00-policy.md`：事实边界和改写规则。
- `01-section-patterns.md`：简历模块通用写法。
- `02-keyword-map.md`：JD 关键词和同义表达。
- `10-*`：常见职业画像，包括教育、编程、硬件、产品、运营、销售、设计、财务、人事、服务等。

## 导入建议

- 每个 Markdown 文件作为一个 Document。
- YAML front matter 写入 `Document.metadata`。
- 必保留字段：`source`、`module`、`kind`、`tags`。
- 当前内容适合 500 字左右 chunk 切分，chunk overlap 可设 50。
- 不要把 `[占位符]` 原样写进简历，必须替换成用户真实信息。

## 使用边界

RAG 只提供表达方式、关键词映射和职业理解，不提供虚构经历。

禁止从知识库直接生成：

- 虚构公司、学校、项目、证书。
- 虚构时间、职位、岗位级别。
- 虚构业绩数字、营收、用户量、性能指标。
- 用户没有提供过的技术栈或业务经验。
