from src.app.agent.constants import AgentStage, AgentStatus
from src.app.agent.state import ResumeAgentState
from src.app.agent.types import ApprovalPayload, ResumeSectionPatch


async def approval_packager_node(state: ResumeAgentState) -> ResumeAgentState:
    """ApprovalPackager Agent：封装待确认 patch。"""
    input_state = dict(state)
    state = dict(state)
    # MessagesState 的 messages 由 reducer 维护；不新增消息时不要原样返回。
    state.pop("messages", None)
    state["current_stage"] = AgentStage.APPROVAL_PACKAGER
    state["status"] = AgentStatus.WAITING_CONFIRM
    payload = build_approval_payload(state)
    state["approval_payload"] = payload.model_dump(by_alias=True)
    return state


def build_approval_payload(state: ResumeAgentState) -> ApprovalPayload:
    """根据通过审查的 patch 构造审批包。"""
    # candidate_patches 在 state 里通常是 dict，打包前统一还原为 Pydantic 模型。
    patches = [
        patch if isinstance(patch, ResumeSectionPatch) else ResumeSectionPatch.model_validate(patch)
        for patch in state.get("candidate_patches", [])
    ]
    return ApprovalPayload(
        runId=state["run_id"],
        resumeId=state["resume_id"],
        summary=build_approval_summary(state),
        riskNotes=state.get("review_notes", []),
        patches=patches,
    )


def build_approval_summary(state: ResumeAgentState) -> str:
    """生成给用户看的审批摘要。"""
    patch_count = len(state.get("candidate_patches", []))
    if patch_count == 0:
        return "当前流程已跑通，但尚未生成可应用的简历修改项。"
    return f"已生成 {patch_count} 个待确认修改项。"
