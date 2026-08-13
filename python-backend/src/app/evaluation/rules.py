"""确定性质量检查：免费、快、可复现，不调用 LLM。

每个 check 返回 RuleResult{passed, signal, detail}。
signal 使用 rubric.FLAG_KEYS 里的标准 key（fabricated / too_large_change /
schema_changed / low_value），scoring_service 据此合成维度分并决定 FLAG。

策略：事实保真检查“宁误报不漏报”——把疑似新增的指标标记出来让人复核，
比放过编造更安全。
"""
from __future__ import annotations

import difflib
import re
from typing import Any

# 技术栈词表：after 里新出现这些词算“疑似新增事实特征”。
TECH_KEYWORDS = {
    "python", "java", "go", "rust", "c++", "c#", "php", "javascript", "typescript",
    "vue", "react", "angular", "spring", "springboot", "django", "flask", "fastapi",
    "mysql", "postgresql", "redis", "mongodb", "kafka", "rabbitmq", "elasticsearch",
    "docker", "kubernetes", "k8s", "nginx", "git", "jenkins", "hadoop", "spark",
    "flink", "rag", "langchain", "agent", "llm", "gpt", "pytorch", "tensorflow",
    "mimo", "qdrant", "minio", "nacos", "grpc", "graphql", "jwt", "oauth",
}

_NUMBER_RE = re.compile(r"\d+(?:\.\d+)?%?")
_YEAR_RE = re.compile(r"(?:19|20)\d{2}")


class RuleResult:
    """单条检查结果。"""

    def __init__(self, name: str, passed: bool, signal: str | None = None, detail: str = "") -> None:
        self.name = name
        self.passed = passed
        self.signal = signal
        self.detail = detail

    def as_dict(self) -> dict[str, Any]:
        return {"passed": self.passed, "signal": self.signal, "detail": self.detail}


def flatten_json(obj: Any) -> str:
    """把 JSON 递归展平为纯文本，便于关键词/数字统计。"""
    if isinstance(obj, str):
        return obj
    if isinstance(obj, dict):
        return "\n".join(flatten_json(v) for v in obj.values() if v is not None)
    if isinstance(obj, list):
        return "\n".join(flatten_json(v) for v in obj if v is not None)
    return str(obj)


def extract_number_tokens(text: str) -> set[str]:
    """提取文本里的数字/百分比 token。"""
    return set(_NUMBER_RE.findall(text))


def extract_year_tokens(text: str) -> set[str]:
    """提取文本里的年份 token。"""
    return set(_YEAR_RE.findall(text))


def extract_tech_tokens(text: str) -> set[str]:
    """提取文本里命中的技术栈词。"""
    lower = text.lower()
    return {kw for kw in TECH_KEYWORDS if kw in lower}


def check_fact_fidelity(before: Any, after: Any, exclude: list[str] | tuple[str, ...] = ()) -> RuleResult:
    """幻觉检测：after 不得引入 before 不存在的新事实特征（数字/年份/技术栈）。

    exclude 里可放 JD 关键词（关键词贴合不算编造，由调用方传入 missing keywords）。
    """
    before_text = flatten_json(before)
    after_text = flatten_json(after)
    new_nums = extract_number_tokens(after_text) - extract_number_tokens(before_text)
    new_years = extract_year_tokens(after_text) - extract_year_tokens(before_text)
    new_tech = extract_tech_tokens(after_text) - extract_tech_tokens(before_text)

    excluded = set()
    for item in exclude:
        excluded |= extract_number_tokens(str(item))
        excluded |= extract_year_tokens(str(item))
        excluded |= extract_tech_tokens(str(item))
    new_nums -= excluded
    new_years -= excluded
    new_tech -= excluded

    evidence: list[str] = []
    if new_nums:
        evidence.append(f"新增量化指标 {sorted(new_nums)}")
    if new_years:
        evidence.append(f"新增年份 {sorted(new_years)}")
    if new_tech:
        evidence.append(f"新增技术栈词 {sorted(new_tech)}")
    if evidence:
        detail = "afterJson 出现原文不存在的新事实特征: " + "；".join(evidence)
        return RuleResult("fact_fidelity", False, "fabricated", detail)
    return RuleResult("fact_fidelity", True, detail="未发现新增事实特征")


def check_keyword_coverage(before: Any, after: Any, keywords: list[str]) -> RuleResult:
    """JD 关键词覆盖：after 相比 before 对 missing 关键词的命中增量。"""
    before_text = flatten_json(before).lower()
    after_text = flatten_json(after).lower()
    new_hits = [kw for kw in keywords if kw and kw.lower() in after_text and kw.lower() not in before_text]
    if new_hits:
        return RuleResult(
            "keyword_coverage", True, "jd_gain",
            f"新增覆盖 {len(new_hits)} 个 JD 关键词: {new_hits}",
        )
    return RuleResult("keyword_coverage", True, detail="未新增 JD 关键词覆盖（可能已是保守改写）")


def check_change_magnitude(before: Any, after: Any) -> RuleResult:
    """改动幅度：变化 <5% 视为低价值，>60% 视为全盘重写需复核。"""
    before_text = flatten_json(before)
    after_text = flatten_json(after)
    if not before_text and not after_text:
        return RuleResult("change_reasonableness", True, detail="空内容")
    ratio = difflib.SequenceMatcher(None, before_text, after_text).ratio()
    change = 1 - ratio
    if change < 0.05:
        return RuleResult("change_reasonableness", False, "low_value", f"改动幅度仅 {change:.1%}，几乎未改，价值低")
    if change > 0.60:
        return RuleResult("change_reasonableness", False, "too_large_change", f"改动幅度达 {change:.1%}，接近全盘重写")
    return RuleResult("change_reasonableness", True, detail=f"改动幅度 {change:.1%}，适中")


def check_specificity(after: Any) -> RuleResult:
    """量化程度：内容里是否有具体数字/百分比（继承自原文的）。"""
    text = flatten_json(after)
    numbers = extract_number_tokens(text)
    if numbers:
        return RuleResult("expression", True, "specific", f"包含量化表述 {sorted(numbers)}")
    return RuleResult("expression", True, detail="缺乏量化表述（不强制，仅提示）")


def check_schema_stability(before: Any, after: Any) -> RuleResult:
    """结构合规：afterJson 顶层 key 集合与 beforeJson 一致。"""
    if not isinstance(before, dict) or not isinstance(after, dict):
        return RuleResult("schema_stability", True, detail="非对象内容，跳过")
    before_keys = set(before.keys())
    after_keys = set(after.keys())
    if after_keys != before_keys:
        missing = sorted(before_keys - after_keys)
        extra = sorted(after_keys - before_keys)
        return RuleResult(
            "schema_stability", False, "schema_changed",
            f"结构变化，缺失字段 {missing}，新增字段 {extra}",
        )
    return RuleResult("schema_stability", True, detail="结构一致")


def run_all_rules(
    before: Any,
    after: Any,
    missing_keywords: list[str] | None = None,
) -> dict[str, dict[str, Any]]:
    """跑全部确定性检查，返回 {check_name: {passed, signal, detail}}。"""
    return {
        "fact_fidelity": check_fact_fidelity(before, after, exclude=missing_keywords or []).as_dict(),
        "keyword_coverage": check_keyword_coverage(before, after, missing_keywords or []).as_dict(),
        "change_magnitude": check_change_magnitude(before, after).as_dict(),
        "specificity": check_specificity(after).as_dict(),
        "schema_stability": check_schema_stability(before, after).as_dict(),
    }
