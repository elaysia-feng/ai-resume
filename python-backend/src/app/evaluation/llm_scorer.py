"""LLM rubric 打分：按 5 维 rubric 给每个 patch 打 1-5 分并给出判定。

复用现有 agent_factory.invoke_agent 通道（与各 agent 节点一致），不新增 LLM 配置。
开关：settings.agent_quality_llm_scoring（env AGENT_QUALITY_LLM_SCORING），
脚本可用 --no-llm-score 关闭。
"""
from __future__ import annotations

import json

from langchain_core.messages import HumanMessage, SystemMessage
from pydantic import BaseModel, Field

from src.app.agent.types import ResumeSectionPatch
from src.app.evaluation.rubric import DIMENSIONS
from src.app.service.agent_factory import agent_factory


class LlmQualityVerdict(BaseModel):
    """LLM 对单个 patch 的 rubric 打分结果。"""

    dimension_scores: dict[str, int] = Field(
        ...,
        description=f"各维度 1-5 分，维度：{list(DIMENSIONS)}",
    )
    verdict: str = Field(..., description="PASS 或 FLAG")
    reason: str = Field(..., description="一句话评语，说明主要扣分点或亮点")


LLM_QUALITY_SYSTEM_PROMPT = """你是简历优化质量的独立评审员。只做两件事：
1. 对比“原文”与“改写后”，按 5 个维度打分（1-5 分）。
2. 给出 PASS（可以直接接受）或 FLAG（需要人工复核）的判定和一句话理由。

维度与判据：
- fact_fidelity（事实保真）：改写是否引入了原文不存在的新事实/指标/年份/技术栈。编造 = 1 分，直接 FLAG。
- jd_match（JD 匹配）：是否把已有事实向岗位 JD 靠拢，是否覆盖 JD 关键要求。注意：不能靠编造来匹配。
- expression（表达质量）：是否更专业、主动、精炼、量化；有没有空话套话。
- change_reasonableness（改动合理性）：改动幅度是否适中；几乎没改或全盘重写都要扣分。
- schema_stability（结构合规）：JSON 结构与原文是否一致。

铁律：简历事实只能来自原文；JD 和参考片段只用来调整表达方式。
如果改写“为了让简历更匹配 JD 而凭空增加了经历、指标、技术栈”，这是编造，
fact_fidelity 必须给 1 分并 FLAG。"""


def build_llm_scorer_messages(
    patch: ResumeSectionPatch,
    job_description: str,
    gap_report: dict,
    retrieved_chunks: list[dict],
) -> list:
    return [
        SystemMessage(content=LLM_QUALITY_SYSTEM_PROMPT),
        HumanMessage(
            content=(
                f"目标岗位 JD:\n{job_description or '(未提供)'}\n\n"
                f"差距报告:\n{json.dumps(gap_report, ensure_ascii=False)[:1500]}\n\n"
                f"改写原文(beforeJson):\n{json.dumps(patch.before_json, ensure_ascii=False)[:3000]}\n\n"
                f"改写结果(afterJson):\n{json.dumps(patch.after_json, ensure_ascii=False)[:3000]}\n\n"
                f"检索参考片段:\n{json.dumps(retrieved_chunks, ensure_ascii=False)[:1500]}"
            )
        ),
    ]


async def llm_score_patch(
    patch: ResumeSectionPatch,
    job_description: str,
    gap_report: dict,
    retrieved_chunks: list[dict],
) -> LlmQualityVerdict:
    """对单个 patch 做 LLM rubric 打分；失败时回退为 PASS（由规则决定是否 FLAG）。"""
    try:
        return await agent_factory.invoke_agent(
            build_llm_scorer_messages(patch, job_description, gap_report, retrieved_chunks),
            LlmQualityVerdict,
        )
    except Exception:
        return LlmQualityVerdict(dimension_scores={}, verdict="PASS", reason="LLM 打分不可用")
