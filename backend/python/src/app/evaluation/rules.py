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

    def __init__(self, name: str, passed: bool, signal: str | None = None, detail: str = "",
                 score: int | None = None) -> None:
        self.name = name
        self.passed = passed
        self.signal = signal
        self.detail = detail
        # 1-5 分，给「这条检查能映射成哪个维度分」的规则用；None 表示不参与打分
        self.score = score

    def as_dict(self) -> dict[str, Any]:
        return {"passed": self.passed, "signal": self.signal, "detail": self.detail,
                "score": self.score}


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


# ---------------------------------------------------------------- 5star 法则
# 项目 / 实习 / 校园经历的每一条描述，按 5 个要素检查：
#   S 情境 Situation：当时的背景、面临的问题
#   T 任务 Task：这条经历里我负责什么
#   A 行动 Action：我具体做了什么
#   R 结果 Result：最终达成了什么
#   Q 量化：有没有可验证的数字
#
# 判定粒度是「一条经历」而不是「一条 bullet」：
# 同一条经历的多个 bullet 可以合起来凑齐 5 个要素，
# 但整条经历讲不清 5 个要素就要扣分。
STAR_ELEMENTS: dict[str, tuple[str, ...]] = {
    "S_情境": (
        "背景", "当时", "面临", "针对", "随着", "由于", "因为", "现状",
        "痛点", "瓶颈", "前提下", "业务场景", "项目背景", "需求场景", "环境下",
    ),
    "T_任务": (
        "负责", "承担", "主导", "牵头", "职责", "目标是", "需要完成",
        "分派", "认领", "受命", "被安排",
    ),
    "A_行动": (
        "设计", "实现", "开发", "重构", "优化", "搭建", "引入", "推动",
        "落地", "编写", "封装", "集成", "抽象", "梳理", "改造", "自研",
        "拆解", "搭建并",
    ),
    "R_结果": (
        "最终", "上线", "交付", "达成", "产出", "沉淀", "支撑", "带动",
        "提升", "降低", "缩短", "节省", "覆盖", "获得", "拿下", "达到",
        "解决了", "实现从", "支撑了",
    ),
}

# 参与 STAR 检查的模块：这些是「讲经历」的模块，其它（技能/证书/教育/总结）不适用
STAR_SECTION_CODES = {"EXPERIENCE", "INTERNSHIP", "PROJECTS", "CAMPUS",
                      "WORK_EXPERIENCE", "PROJECT_EXPERIENCE"}

# 只看正文字段的元数据：这些是「这条经历叫什么」，不是 STAR 内容
STAR_METADATA_FIELDS = {"name", "company", "organization", "position", "role",
                         "dateRange", "startDate", "endDate", "techStack", "url", "link"}


def _star_body_text(item: dict[str, Any]) -> str:
    """取一条经历里的正文（highlights / description 之类的描述性字段）。"""
    parts: list[str] = []
    for key, value in item.items():
        if key in STAR_METADATA_FIELDS:
            continue
        if isinstance(value, str):
            parts.append(value)
        elif isinstance(value, list):
            parts.extend(v for v in value if isinstance(v, str))
        elif isinstance(value, (int, float)):
            parts.append(str(value))
    return " ".join(parts)


def extract_experience_items(node: Any) -> list[dict[str, Any]]:
    """找出「条目列表」：结构是 {items: [ {...}, ... ]}。

    简历模块的 JSON 结构千差万别，这里只做通用兜底 ——
    取最外层那个「元素全是 dict」的数组。
    """
    if isinstance(node, dict):
        for value in node.values():
            if isinstance(value, list) and value and all(isinstance(x, dict) for x in value):
                return value
        for value in node.values():
            found = extract_experience_items(value)
            if found:
                return found
    return []


def check_star_quality(after: Any, section_code: str = "") -> RuleResult:
    """5star 法则：每条经历按 STAR + 量化 五个要素查缺。

    Args:
        after: 改写后的模块内容 JSON。
        section_code: 模块编码；非经历类模块直接跳过。

    Returns:
        带 1-5 分的检查结果（分越高说明五要素越齐）。
    """
    if section_code and section_code not in STAR_SECTION_CODES:
        return RuleResult("star_quality", True, detail=f"模块 {section_code} 不适用 STAR 检查，跳过")

    items = extract_experience_items(after)
    if not items:
        return RuleResult("star_quality", True, detail="没有可检查的经历条目，跳过")

    total_elements = len(STAR_ELEMENTS) + 1  # +1 是 Q_量化
    missing_per_item: list[list[str]] = []
    for item in items:
        text = _star_body_text(item)
        missing: list[str] = []
        for element, hints in STAR_ELEMENTS.items():
            if not any(hint in text for hint in hints):
                missing.append(element)
        if not extract_number_tokens(text):
            missing.append("Q_量化")
        missing_per_item.append(missing)

    avg_ratio = sum((total_elements - len(m)) / total_elements for m in missing_per_item) / len(missing_per_item)
    score = max(1, min(5, 1 + round(avg_ratio * 4)))

    incomplete = [(idx, m) for idx, m in enumerate(missing_per_item) if m]
    if not incomplete:
        return RuleResult("star_quality", True, None,
                          f"{len(items)} 条经历全部满足 STAR + 量化", score=5)
    detail = "缺 STAR 要素：" + "；".join(
        f"第{idx + 1}条缺 {'、'.join(missing)}" for idx, missing in incomplete
    )
    return RuleResult("star_quality", False, "star_missing", detail, score=score)


def run_all_rules(
    before: Any,
    after: Any,
    missing_keywords: list[str] | None = None,
    section_code: str = "",
) -> dict[str, dict[str, Any]]:
    """跑全部确定性检查，返回 {check_name: {passed, signal, detail, score}}。"""
    return {
        "fact_fidelity": check_fact_fidelity(before, after, exclude=missing_keywords or []).as_dict(),
        "keyword_coverage": check_keyword_coverage(before, after, missing_keywords or []).as_dict(),
        "change_magnitude": check_change_magnitude(before, after).as_dict(),
        "specificity": check_specificity(after).as_dict(),
        "schema_stability": check_schema_stability(before, after).as_dict(),
        "star_quality": check_star_quality(after, section_code).as_dict(),
    }
