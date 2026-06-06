from typing import Any

from langchain_core.messages import HumanMessage, SystemMessage
from pydantic import BaseModel, Field

from src.app.agent.constants import AgentStage
from src.app.agent.memory import build_memory_messages, compact_memory_for_model
from src.app.agent.prompts.rewriter_prompt import REWRITER_SYSTEM_PROMPT
from src.app.agent.state import ResumeAgentState
from src.app.agent.types import ResumeSectionPatch
from src.app.service.patch_builder_service import patch_builder_service
from src.app.service.agent_factory import agent_factory


class SectionPatchList(BaseModel):
    """Rewriter Agent 输出的 patch 列表。"""

    patches: list[ResumeSectionPatch] = Field(default_factory=list, description="生成的简历模块修改提案列表")


async def rewriter_node(state: ResumeAgentState) -> ResumeAgentState:
    """Rewriter Agent：生成 section 级简历修改 patch。"""
    input_state = dict(state)
    state = dict(state)
    # rewriter 的产物是 candidate_patches，不直接修改 resume_snapshot。
    state.pop("messages", None)
    state["current_stage"] = AgentStage.REWRITER

    patches: list[ResumeSectionPatch] = []
    for section in select_target_sections(state):
        try:
            # 每个 section 独立生成 patch，方便用户后续按模块确认或拒绝。
            messages = build_rewriter_messages(input_state, section)
            memory_updates, input_state = await compact_memory_for_model(input_state, messages)
            state.update(memory_updates)
            response = await agent_factory.invoke_agent(build_rewriter_messages(input_state, section), SectionPatchList)
            patches.extend(response.patches)
        except Exception:
            # 单个 section 失败不阻断其他模块，最终由 reviewer 判断是否有可用 patch。
            continue

    state["candidate_patches"] = [
        patch.model_dump(by_alias=True)
        for patch in filter_target_patches(
            patch_builder_service.filter_empty_patches(patches),
            state.get("target_section_id"),
        )
    ]
    return state


def select_target_sections(state: ResumeAgentState) -> list[dict]:
    """选择本轮需要改写的简历模块。"""
    sections = state.get("resume_snapshot", {}).get("sections", [])
    target_section_id = state.get("target_section_id")
    if not target_section_id:
        return []
    # v1 每次 run 只处理当前编辑模块，避免 AI 越权改完整简历。
    return [section for section in sections if _section_id(section) == target_section_id]


def build_rewriter_messages(state: ResumeAgentState, section_snapshot: dict) -> list:
    """构造 Rewriter Agent 的模型消息。"""
    return [
        SystemMessage(content=REWRITER_SYSTEM_PROMPT),
        *build_memory_messages(state),
        HumanMessage(
            content=(
                f"目标模块：{section_snapshot}\n"
                f"targetSectionId：{state.get('target_section_id')}\n"
                f"JD 分析：{state.get('jd_analysis', {})}\n"
                f"差距报告：{state.get('gap_report', {})}\n"
                f"检索异常：{state.get('retrieval_error') or '无'}\n"
                f"参考片段：{state.get('retrieved_chunks', [])}"
            )
        ),
    ]


def parse_section_patches(raw_response: object) -> list[ResumeSectionPatch]:
    """解析 Rewriter 输出的 patch 列表。"""
    if isinstance(raw_response, dict):
        raw_response = raw_response.get("patches", [])
    return [ResumeSectionPatch.model_validate(item) for item in raw_response or []]


def filter_target_patches(patches: list[ResumeSectionPatch], target_section_id: int | None) -> list[ResumeSectionPatch]:
    """只保留当前模块的 patch。"""
    if not target_section_id:
        return []
    return [patch for patch in patches if patch.section_id == target_section_id]


def _section_id(section: dict[str, Any]) -> int | None:
    """兼容 Java 返回的 id / sectionId 两种字段命名。"""
    return section.get("id") or section.get("sectionId")
