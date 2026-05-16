from typing import Any

from src.app.agent.constants import PatchOperation
from src.app.agent.types import ResumeSectionPatch


class PatchBuilderService:
    """ResumeSectionPatch 构建和清洗服务。

    这里放 patch 的机械组装、过滤、备注合并等纯工具逻辑。
    哪些 section 要改、何时给用户确认，仍由 LangGraph 节点决定。
    """

    def build_patch_id(self, section_id: int, index: int) -> str:
        """生成 patchId。"""
        # patchId 只需要在本轮 run 内可追踪，前端用它做确认/拒绝。
        return f"patch-{section_id}-{index}"

    def build_patch(
        self,
        *,
        section_snapshot: dict[str, Any],
        after_json: dict[str, Any],
        reason: str,
        change_summary: str,
    ) -> ResumeSectionPatch:
        """根据 section 快照和改写结果构造 patch。"""
        section_id = section_snapshot.get("id") or section_snapshot.get("sectionId")
        # beforeJson 固定来自原 section，Java 应用 patch 时可用它做冲突检测。
        return ResumeSectionPatch(
            patchId=self.build_patch_id(int(section_id), 1),
            sectionId=section_id,
            sectionCode=section_snapshot.get("sectionCode") or section_snapshot.get("section_code") or "",
            sectionTitle=section_snapshot.get("sectionTitle") or section_snapshot.get("section_title") or "",
            operation=PatchOperation.REPLACE_SECTION_CONTENT,
            reason=reason,
            beforeJson=section_snapshot.get("contentJson") or section_snapshot.get("content_json") or {},
            afterJson=after_json,
            changeSummary=change_summary,
            riskLevel="LOW",
        )

    def build_approval_payload(self, run_id: int, resume_id: int, patches: list[ResumeSectionPatch]) -> dict:
        """构造等待用户确认的 approval payload。"""
        return {
            "runId": run_id,
            "resumeId": resume_id,
            "summary": f"已生成 {len(patches)} 个待确认修改项",
            "riskNotes": [],
            "patches": [patch.model_dump(by_alias=True) for patch in patches],
        }

    def normalize_llm_patch_output(self, raw_output: dict) -> list[ResumeSectionPatch]:
        """规范化 LLM 输出为 patch 列表。"""
        patches = raw_output.get("patches", raw_output if isinstance(raw_output, list) else [])
        return [ResumeSectionPatch.model_validate(patch) for patch in patches]

    def filter_empty_patches(self, patches: list[ResumeSectionPatch]) -> list[ResumeSectionPatch]:
        """过滤无实际变化的 patch。"""
        # LLM 有时会返回“看似有建议但内容没变”的 patch，这里先过滤掉。
        return [patch for patch in patches if patch.before_json != patch.after_json]

    def merge_review_notes(self, approval_payload: dict, review_notes: list[str]) -> dict:
        """把 reviewer 备注合并到 approval payload。"""
        payload = dict(approval_payload)
        payload["riskNotes"] = [*payload.get("riskNotes", []), *review_notes]
        return payload


patch_builder_service = PatchBuilderService()
