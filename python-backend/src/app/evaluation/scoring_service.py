"""统一评分入口：确定性规则 + 可选 LLM rubric → 每个 patch 的 0-100 分与 PASS/FLAG。

分数公式：每维度 1-5 分 → 归一化 (dim-1)/4 → 按 rubric.DIMENSION_WEIGHTS 加权 → *100。
事实保真违规时封顶 FACT_FIDELITY_CAP，避免其他维度把编造的结果拉高。
FLAG 条件：编造 / 改动过大 / 结构变化 / 低价值 / 分数低于阈值 / LLM 判 FLAG。
"""
from __future__ import annotations

from src.app.agent.types import ResumeSectionPatch
from src.app.config.settings import get_settings
from src.app.evaluation import rules
from src.app.evaluation.rubric import (
    DIMENSION_WEIGHTS,
    DIMENSIONS,
    FACT_FIDELITY_CAP,
    FLAG_KEYS,
    PASS_THRESHOLD,
)
from src.app.evaluation.types import PatchQuality, RunResult


def _clamp_dimension(value: object, default: int = 3) -> int:
    try:
        v = int(value)  # type: ignore[arg-type]
    except (TypeError, ValueError):
        return default
    return max(1, min(5, v))


def _dimension_from_rules(check_results: dict[str, dict]) -> dict[str, int]:
    """把确定性检查信号映射成 1-5 分（LLM 不可用时的兜底）。"""
    ff = check_results.get("fact_fidelity", {})
    cov = check_results.get("keyword_coverage", {})
    mag = check_results.get("change_magnitude", {})
    spec = check_results.get("specificity", {})
    sch = check_results.get("schema_stability", {})
    return {
        "fact_fidelity": 1 if ff.get("signal") == "fabricated" else 5,
        "jd_match": 5 if cov.get("signal") == "jd_gain" else 3,
        "change_reasonableness": {"low_value": 2, "too_large_change": 2}.get(mag.get("signal"), 4),
        "expression": 4 if spec.get("signal") == "specific" else 3,
        "schema_stability": 1 if sch.get("signal") == "schema_changed" else 5,
    }


def compute_score(dimension_scores: dict[str, int]) -> int:
    """把维度分合成 0-100。缺失维度按 3 分（中性）处理。"""
    total = sum(DIMENSION_WEIGHTS[dim] * (dimension_scores.get(dim, 3) - 1) / 4 for dim in DIMENSIONS)
    return round(total * 100)


def _signals(check_results: dict[str, dict]) -> set[str]:
    return {res.get("signal") for res in check_results.values() if res.get("signal")}


async def score_patch(
    patch: ResumeSectionPatch,
    *,
    missing_keywords: list[str],
    job_description: str,
    gap_report: dict,
    retrieved_chunks: list[dict],
    use_llm: bool,
) -> PatchQuality:
    """对单个 patch 打分：规则优先，LLM 可覆盖规则分（更高保真）。"""
    check_results = rules.run_all_rules(patch.before_json, patch.after_json, missing_keywords)
    rule_dimensions = _dimension_from_rules(check_results)

    llm_review: str | None = None
    llm_dimensions: dict[str, int] = {}
    if use_llm and get_settings().agent_quality_llm_scoring:
        try:
            from src.app.evaluation.llm_scorer import llm_score_patch

            verdict = await llm_score_patch(patch, job_description, gap_report, retrieved_chunks)
            llm_review = verdict.reason
            for dim in DIMENSIONS:
                if dim in verdict.dimension_scores:
                    llm_dimensions[dim] = _clamp_dimension(verdict.dimension_scores[dim])
            if verdict.verdict == "FLAG" and llm_review and "LLM 打分不可用" not in llm_review:
                llm_review = f"LLM 判定需复核: {llm_review}"
        except Exception:
            pass

    merged = {dim: llm_dimensions.get(dim, rule_dimensions.get(dim, 3)) for dim in DIMENSIONS}

    score = compute_score(merged)
    signals = _signals(check_results)

    flag_reasons: list[str] = []
    if "fabricated" in signals:
        flag_reasons.append(check_results["fact_fidelity"]["detail"])
    if "too_large_change" in signals:
        flag_reasons.append(check_results["change_magnitude"]["detail"])
    if "schema_changed" in signals:
        flag_reasons.append(check_results["schema_stability"]["detail"])
    if "low_value" in signals:
        flag_reasons.append(check_results["change_magnitude"]["detail"])
    if llm_review and "LLM 打分不可用" not in llm_review:
        flag_reasons.append(llm_review)

    if "fabricated" in signals:
        score = min(score, FACT_FIDELITY_CAP)
    verdict = "FLAG" if (score < PASS_THRESHOLD or bool(signals & FLAG_KEYS)) else "PASS"

    return PatchQuality(
        patch_id=patch.patch_id,
        score=score,
        verdict=verdict,
        flag_reasons=flag_reasons,
        dimension_scores=merged,
        checks=check_results,
        llm_review=llm_review if (llm_review and "LLM 打分不可用" not in llm_review) else None,
    )


async def score_run(
    run: RunResult,
    *,
    missing_keywords: list[str],
    job_description: str,
    gap_report: dict,
    use_llm: bool = True,
) -> RunResult:
    """对一次运行的所有 patch 打分，并聚合 run 总分。"""
    run.quality = [
        await score_patch(
            patch,
            missing_keywords=missing_keywords,
            job_description=job_description,
            gap_report=gap_report,
            retrieved_chunks=run.retrieved_chunks,
            use_llm=use_llm,
        )
        for patch in run.patches
    ]
    run.overall_score = round(sum(q.score for q in run.quality) / len(run.quality), 1) if run.quality else 0.0
    return run
