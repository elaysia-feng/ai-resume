"""评分维度与权重定义 —— 这是“简历优化结果算不算好”的客观标准。

每个维度 1-5 分（LLM rubric 或由确定性规则映射），由 scoring_service 按权重
合成 0-100 分。事实保真权重最高，因为简历优化的第一红线是“不编造”。

维度和权重在此处统一定义，rules / llm_scorer / scoring_service 都引用它，
避免三处各写一份导致口径漂移。
"""

# 评分维度（顺序即展示顺序）。
DIMENSIONS = (
    "fact_fidelity",          # 事实保真：是否引入原文不存在的新事实/指标
    "jd_match",               # JD 匹配：是否把已有事实向岗位要求靠拢
    "expression",             # 表达质量：是否更专业、主动、精炼、量化
    "change_reasonableness",  # 改动合理性：改动幅度是否适中
    "schema_stability",       # 结构合规：JSON 结构是否与原文一致
)

DIMENSION_WEIGHTS: dict[str, float] = {
    "fact_fidelity": 0.35,
    "jd_match": 0.25,
    "expression": 0.20,
    "change_reasonableness": 0.15,
    "schema_stability": 0.05,
}

DIMENSION_LABELS: dict[str, str] = {
    "fact_fidelity": "事实保真",
    "jd_match": "JD 匹配",
    "expression": "表达质量",
    "change_reasonableness": "改动合理性",
    "schema_stability": "结构合规",
}

# 事实保真违规时的分数封顶，避免其他维度把编造的结果拉高。
FACT_FIDELITY_CAP = 40

# 确定性检查的信号 key；命中任一 → 判 FLAG。
FLAG_KEYS = {"fabricated", "too_large_change", "schema_changed", "low_value"}

# 低于该分数视为需要人工复核。
PASS_THRESHOLD = 60
