from src.app.agent.types import ResumeSectionPatch
from src.app.service.schema_validate_service import schema_validate_service


async def validate_section_schema_tool(patch: ResumeSectionPatch, schemas: dict) -> None:
    """校验 patch 的 afterJson 是否符合 section schema。"""
    schema_validate_service.validate_section_schema(patch.section_code, patch.after_json, schemas)
