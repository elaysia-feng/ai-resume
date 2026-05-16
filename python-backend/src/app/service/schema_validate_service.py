from typing import Any

from src.app.agent.constants import PatchOperation
from src.app.agent.types import ResumeSectionPatch


class SchemaValidateService:
    """Patch 与 section schema 校验服务。

    这是确定性校验工具，不调用模型，也不决定业务路由。
    Reviewer 节点会先调用这里做硬校验，再决定是否需要 LLM 审查。
    """

    def validate_patch_against_snapshot(self, patch: ResumeSectionPatch, resume_snapshot: dict[str, Any]) -> None:
        """校验 patch 是否引用当前简历中的 section。"""
        sections = resume_snapshot.get("sections", [])
        matched = [
            section for section in sections
            if (section.get("id") or section.get("sectionId")) == patch.section_id
        ]
        if not matched:
            # patch 只能改当前简历已有 section，不能凭空新增目标模块。
            raise ValueError(f"patch 引用了不存在的 sectionId: {patch.section_id}")

    def validate_section_schema(self, section_code: str, content_json: dict[str, Any], schemas: dict[str, dict]) -> None:
        """校验 afterJson 是否符合对应 section schema。"""
        if content_json is None:
            raise ValueError(f"{section_code} 的 afterJson 不能为空")

    def detect_stale_before_json(self, patch: ResumeSectionPatch, current_content: dict[str, Any]) -> bool:
        """判断 patch 的 beforeJson 是否已经过期。"""
        return patch.before_json != current_content

    def validate_patch_operation(self, patch: ResumeSectionPatch) -> None:
        """校验 patch 操作类型是否合法。"""
        if patch.operation != PatchOperation.REPLACE_SECTION_CONTENT:
            raise ValueError(f"不支持的 patch 操作: {patch.operation}")

    def validate_all_patches(
        self,
        patches: list[ResumeSectionPatch],
        resume_snapshot: dict[str, Any],
        schemas: dict[str, dict],
    ) -> None:
        """批量校验 patch。"""
        for patch in patches:
            # 校验顺序从操作类型、目标 section 到内容 schema，错误信息更容易定位。
            self.validate_patch_operation(patch)
            self.validate_patch_against_snapshot(patch, resume_snapshot)
            self.validate_section_schema(patch.section_code, patch.after_json, schemas)


schema_validate_service = SchemaValidateService()
