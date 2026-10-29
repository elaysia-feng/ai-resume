from app.graphs.resume_agent.types import ResumeSectionPatch
from app.services.java_gateway_service import java_gateway_service

"""patch -> 简历模块修改提案"""
async def preview_patch_tool(resume_id: int, patches: list[ResumeSectionPatch]) -> dict:
    """请求 Java 生成 patch 预览。"""
    patch_payload = [patch.model_dump(by_alias=True) for patch in patches]
    return await java_gateway_service.preview_patch(resume_id, patch_payload)
