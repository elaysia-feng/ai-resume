"""简历优化质量评测系统。

独立的评分模块：确定性规则 + 可选 LLM rubric 打分。
被 scripts/quality_audit.py 调用做批量质量审计与 RAG A/B 对比。
"""
