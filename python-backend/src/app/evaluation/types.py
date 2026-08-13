"""评分系统结果类型。"""
from typing import Any, Literal

from pydantic import BaseModel, Field

from src.app.agent.types import ResumeSectionPatch


class PatchQuality(BaseModel):
    """单个优化 patch 的质量评分结果。"""

    patch_id: str = Field(..., description="对应 ResumeSectionPatch.patchId")
    score: int = Field(..., description="综合质量分 0-100")
    verdict: Literal["PASS", "FLAG"] = Field(..., description="PASS=可批量接受，FLAG=需人工复核")
    flag_reasons: list[str] = Field(default_factory=list, description="命中检查项 + 证据")
    dimension_scores: dict[str, int] = Field(default_factory=dict, description="各评分维度 1-5 分")
    checks: dict[str, Any] = Field(default_factory=dict, description="确定性检查明细")
    llm_review: str | None = Field(default=None, description="LLM 一句话评语")


class RunResult(BaseModel):
    """一次优化运行（某个 case × 某个 RAG 模式）的评分结果。"""

    case_id: str
    title: str
    rag_mode: Literal["on", "off"]
    patches: list[ResumeSectionPatch] = Field(default_factory=list)
    quality: list[PatchQuality] = Field(default_factory=list)
    overall_score: float = Field(default=0.0, description="该 run 所有 patch 的平均分，无 patch 时为 0")
    retrieved_chunks: list[dict[str, Any]] = Field(default_factory=list)
    gap_report: dict[str, Any] = Field(default_factory=dict, description="图内 gap_analyzer 输出的差距报告")
    jd_analysis: dict[str, Any] = Field(default_factory=dict, description="图内 jd_analyst 输出的 JD 分析")
    skipped: str | None = Field(default=None, description="若被跳过（需要补充信息/运行异常），记录原因")


class RagComparison(BaseModel):
    """RAG on/off 总体对比。"""

    paired_cases: int = Field(default=0, description="同时成功跑完 on 与 off 的用例数")
    on_mean: float = Field(default=0.0)
    off_mean: float = Field(default=0.0)
    on_wins: int = Field(default=0)
    off_wins: int = Field(default=0)
    ties: int = Field(default=0)
    dimension_mean: dict[str, dict[str, float]] = Field(default_factory=dict)
    conclusion: str = ""


class AuditReport(BaseModel):
    """整批抽检的汇总报告。"""

    cases: list[RunResult] = Field(default_factory=list)
    rag_summary: RagComparison = Field(default_factory=RagComparison)
    flagged: list[dict[str, Any]] = Field(default_factory=list, description="需人工抽检清单")
