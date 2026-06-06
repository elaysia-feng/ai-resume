from langchain_core.messages import HumanMessage, SystemMessage

from src.app.agent.constants import AgentStage
from src.app.agent.memory import build_memory_messages, compact_memory_for_model
from src.app.agent.prompts.reviewer_prompt import REVIEWER_SYSTEM_PROMPT
from src.app.agent.state import ResumeAgentState
from src.app.agent.types import ResumeSectionPatch, ReviewResult
from src.app.config.settings import get_settings
from src.app.service.agent_factory import agent_factory
from src.app.service.schema_validate_service import schema_validate_service


async def reviewer_node(state: ResumeAgentState) -> ResumeAgentState:
    """Reviewer Agent：审查 patch 的事实一致性和 schema 合法性。"""
    input_state = dict(state)
    state = dict(state)
    # reviewer 只写审查结果和是否通过，不新增 messages。
    state.pop("messages", None)
    state["current_stage"] = AgentStage.REVIEWER
    patches = parse_review_patches(state)
    try:
        # 先做确定性的 schema / sectionId 校验，避免把明显非法 patch 交给 LLM。
        schema_validate_service.validate_all_patches(
            patches,
            state.get("resume_snapshot", {}),
            state.get("section_schemas", {}),
        )
        validate_target_section_only(patches, state.get("target_section_id"))
        if patches:
            # 本地校验通过后，再让 LLM 判断事实一致性和表达风险。
            messages = build_reviewer_messages(input_state)
            memory_updates, input_state = await compact_memory_for_model(input_state, messages)
            state.update(memory_updates)
            result = await agent_factory.invoke_agent(build_reviewer_messages(input_state), ReviewResult)
        else:
            result = ReviewResult(passed=True, notes=["未生成修改提案，跳过 LLM 审查"], rejectedReasons=[])
    except ValueError as exc:
        # 本地校验失败说明 patch 结构或目标模块有问题，应退回 rewriter 重写。
        state["review_retry_count"] = state.get("review_retry_count", 0) + 1
        result = ReviewResult(passed=False, notes=[], rejectedReasons=[str(exc)])
    except Exception as exc:
        # LLM 审查不可用时，只要本地校验已通过，就允许进入审批，但把风险写入 notes。
        result = ReviewResult(passed=True, notes=[f"LLM 审查不可用，已完成本地校验: {exc}"], rejectedReasons=[])

    state["review_result"] = result.model_dump(by_alias=True)
    state["review_passed"] = result.passed
    state["review_notes"] = result.notes if result.passed else result.rejected_reasons
    return state


def build_reviewer_messages(state: ResumeAgentState) -> list:
    """构造 Reviewer Agent 的模型消息。"""
    return [
        SystemMessage(content=REVIEWER_SYSTEM_PROMPT),
        *build_memory_messages(state),
        HumanMessage(
            content=(
                f"目标模块：{get_target_section(state)}\n"
                f"候选 patch：{state.get('candidate_patches', [])}"
            )
        ),
    ]


def parse_review_result(raw_response: object) -> ReviewResult:
    """解析 Reviewer 输出。"""
    return ReviewResult.model_validate(raw_response)


def can_retry_rewrite(state: ResumeAgentState) -> bool:
    """判断是否还能退回 Rewriter 重写。"""
    return state.get("review_retry_count", 0) < get_settings().agent_review_max_retry


def parse_review_patches(state: ResumeAgentState):
    """把 state 中的 patch dict 还原为模型对象。"""
    # rewriter 写入 state 时为了 JSON 友好会转成 dict；reviewer 需要模型对象做校验。
    return [
        item if isinstance(item, ResumeSectionPatch) else ResumeSectionPatch.model_validate(item)
        for item in state.get("candidate_patches", [])
    ]


def validate_target_section_only(patches: list[ResumeSectionPatch], target_section_id: int | None) -> None:
    """Reviewer 硬校验：本轮只能修改 targetSectionId 对应模块。"""
    if not target_section_id:
        raise ValueError("缺少 targetSectionId，不能审查 patch")
    for patch in patches:
        if patch.section_id != target_section_id:
            raise ValueError(f"patch 越权修改了非目标模块: {patch.section_id}")


def get_target_section(state: ResumeAgentState) -> dict:
    """只取本轮允许审查的目标模块，避免把完整简历快照塞给 Reviewer。"""
    target_section_id = state.get("target_section_id")
    for section in state.get("resume_snapshot", {}).get("sections", []):
        section_id = section.get("id") or section.get("sectionId")
        if section_id == target_section_id:
            return section
    return {}
