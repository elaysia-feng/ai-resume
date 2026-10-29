"""5star 法则（STAR + 量化）的确定性检查。

判定粒度是「一条经历」：同一条经历的多个 bullet 可以合起来凑齐 5 个要素。
"""
import pytest

from app.evaluation import rules
from app.evaluation.rubric import DIMENSION_LABELS, DIMENSION_WEIGHTS, DIMENSIONS


def test_full_star_gets_top_score():
    after = {
        "items": [
            {
                "company": "某公司",
                "position": "后端开发",
                "dateRange": "2024-2026",
                "highlights": [
                    "当时线上订单查询很慢，我们面临性能瓶颈，负责主导查询链路重构",
                    "设计并实现新的缓存与索引方案，落地后上线，最终响应时间从 800ms 降低到 120ms",
                ],
            }
        ]
    }
    result = rules.check_star_quality(after, "EXPERIENCE")
    assert result.passed is True
    assert result.signal is None
    assert result.score == 5


def test_missing_elements_are_reported():
    after = {
        "items": [
            {
                "company": "某公司",
                "position": "后端开发",
                "dateRange": "2024-2026",
                "highlights": ["参与了项目的开发工作"],
            }
        ]
    }
    result = rules.check_star_quality(after, "EXPERIENCE")
    assert result.passed is False
    assert result.signal == "star_missing"
    assert result.score < 5
    for element in ("S_情境", "T_任务", "R_结果", "Q_量化"):
        assert element in result.detail


def test_highlights_can_together_cover_all_elements():
    """一条经历的多个 bullet 合起来齐备就应该满分。"""
    after = {
        "items": [
            {
                "company": "某公司",
                "position": "后端开发",
                "dateRange": "2024-2026",
                "highlights": [
                    "当时公司要接入新渠道，我负责设计对接方案",
                    "推动联调上线，最终接通 3 个渠道，日均订单提升 25%",
                ],
            }
        ]
    }
    result = rules.check_star_quality(after, "EXPERIENCE")
    assert result.score == 5, result.detail


def test_non_experience_section_is_skipped():
    after = {"items": [{"name": "Java", "proficiency": "精通"}]}
    result = rules.check_star_quality(after, "SKILLS")
    assert result.passed is True
    assert result.signal is None
    assert "跳过" in result.detail


def test_projects_section_uses_description():
    after = {
        "items": [
            {
                "name": "订单系统重构",
                "role": "负责人",
                "dateRange": "2025-2026",
                "description": "当时旧系统无法支撑大促，面对性能瓶颈负责主导重构，"
                               "设计并落地分库分表方案，上线后接口耗时从 1s 降到 80ms",
            }
        ]
    }
    result = rules.check_star_quality(after, "PROJECTS")
    assert result.score == 5, result.detail


def test_no_items_is_skipped():
    result = rules.check_star_quality({"items": []}, "EXPERIENCE")
    assert result.passed is True
    assert result.signal is None


def test_rule_is_wired_into_run_all_rules():
    before = {"items": []}
    after = {"items": [{"company": "A", "position": "B", "dateRange": "C", "highlights": ["做过一些开发"]}]}
    results = rules.run_all_rules(before, after, [], section_code="EXPERIENCE")
    assert "star_quality" in results
    assert "score" in results["star_quality"]


def test_rubric_weights_cover_star_and_sum_to_one():
    assert "star_quality" in DIMENSIONS
    assert DIMENSION_LABELS["star_quality"] == "5star 法则"
    assert sum(DIMENSION_WEIGHTS.values()) == pytest.approx(1.0)
    assert set(DIMENSION_WEIGHTS) == set(DIMENSIONS)


def _patch(after):
    from app.graphs.resume_agent.types import ResumeSectionPatch

    return ResumeSectionPatch(
        patchId="p1",
        sectionId=1,
        sectionCode="EXPERIENCE",
        sectionTitle="工作经历",
        operation="REPLACE_SECTION_CONTENT",
        reason="测试",
        beforeJson={"items": []},
        afterJson=after,
        changeSummary="测试",
        riskLevel="LOW",
    )


def test_score_patch_carries_star_dimension():
    """5star 维度要真的进到 patch 的维度分里。"""
    import asyncio

    from app.evaluation.scoring_service import score_patch

    weak = {
        "items": [
            {
                "company": "某公司",
                "position": "后端开发",
                "dateRange": "2024-2026",
                "highlights": ["参与了一些开发工作"],
            }
        ]
    }
    strong = {
        "items": [
            {
                "company": "某公司",
                "position": "后端开发",
                "dateRange": "2024-2026",
                "highlights": [
                    "当时线上订单查询很慢，我们面临性能瓶颈，负责主导查询链路重构",
                    "设计并实现新的缓存与索引方案，落地后上线，最终响应时间从 800ms 降低到 120ms",
                ],
            }
        ]
    }

    weak_quality = asyncio.run(
        score_patch(_patch(weak), missing_keywords=[], job_description="", gap_report={},
                    use_llm=False)
    )
    strong_quality = asyncio.run(
        score_patch(_patch(strong), missing_keywords=[], job_description="", gap_report={},
                    use_llm=False)
    )

    assert "star_quality" in weak_quality.dimension_scores
    assert weak_quality.dimension_scores["star_quality"] < strong_quality.dimension_scores["star_quality"]
    assert strong_quality.dimension_scores["star_quality"] == 5
    assert "star_quality" in weak_quality.checks
