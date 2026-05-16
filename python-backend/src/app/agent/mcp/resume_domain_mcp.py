from typing import Any

from mcp.server.fastmcp import FastMCP

from src.app.agent.types import ResumeSectionPatch
from src.app.internal_dto.bootstrap_request import BootstrapRequest
from src.app.internal_dto.bootstrap_response import BootstrapResponse
from src.app.internal_dto.run_status_update_request import RunStatusUpdateRequest
from src.app.service.java_gateway_service import java_gateway_service
from src.app.service.schema_validate_service import schema_validate_service


mcp = FastMCP("ai-resume-forge-domain")


class ResumeDomainMcpAdapter:
    """简历业务域 MCP 适配器。

    这个类保留给项目内部 Python 代码直接调用。
    下面的 @mcp.tool() 函数才是真正暴露给 MCP Client 的工具入口。
    """

    async def load_resume_context(self, request: BootstrapRequest) -> BootstrapResponse:
        """读取简历上下文。"""
        return await java_gateway_service.load_bootstrap_context(request)

    async def validate_patch(self, patch: ResumeSectionPatch) -> None:
        """校验 AI 生成的简历修改提案。"""
        schema_validate_service.validate_patch_operation(patch)

    async def preview_patch(self, resume_id: int, patches: list[ResumeSectionPatch]) -> dict:
        """预览 AI 生成的简历修改提案。"""
        patch_payload = [patch.model_dump(by_alias=True) for patch in patches]
        return await java_gateway_service.preview_patch(resume_id, patch_payload)

    async def persist_events(self, run_id: int, events: list[dict]) -> None:
        """持久化 run 事件。"""
        await java_gateway_service.post_json(f"/internal/agent/runs/{run_id}/events/batch", {"events": events})

    async def update_run_status(self, run_id: int, status: str, payload: dict) -> None:
        """更新 run 状态。"""
        await java_gateway_service.update_run_status(run_id, RunStatusUpdateRequest(status=status, **payload))


resume_domain_mcp_adapter = ResumeDomainMcpAdapter()


@mcp.tool()
async def load_resume_context(run_id: int, session_id: int, resume_id: int, scene_code: str) -> dict[str, Any]:
    """加载简历、schema、历史消息等 Agent 上下文。"""
    request = BootstrapRequest(
        runId=run_id,
        sessionId=session_id,
        resumeId=resume_id,
        sceneCode=scene_code,
    )
    response = await resume_domain_mcp_adapter.load_resume_context(request)
    return response.model_dump(by_alias=True)


@mcp.tool()
async def validate_patch(
    patch: dict[str, Any],
    resume_snapshot: dict[str, Any] | None = None,
    schemas: dict[str, dict[str, Any]] | None = None,
) -> dict[str, Any]:
    """校验 AI 生成的简历修改提案是否合法。"""
    patch_model = ResumeSectionPatch.model_validate(patch)
    schema_validate_service.validate_patch_operation(patch_model)

    if resume_snapshot is not None:
        schema_validate_service.validate_patch_against_snapshot(patch_model, resume_snapshot)

    if schemas is not None:
        schema_validate_service.validate_section_schema(
            patch_model.section_code,
            patch_model.after_json,
            schemas,
        )

    return {"valid": True, "patchId": patch_model.patch_id}


@mcp.tool()
async def preview_patch(resume_id: int, patches: list[dict[str, Any]]) -> dict[str, Any]:
    """请求 Java 预览 AI 生成的简历修改提案。"""
    patch_models = [ResumeSectionPatch.model_validate(patch) for patch in patches]
    return await resume_domain_mcp_adapter.preview_patch(resume_id, patch_models)


@mcp.tool()
async def persist_run_events(run_id: int, events: list[dict[str, Any]]) -> dict[str, Any]:
    """批量持久化 Agent run 事件到 Java。"""
    await resume_domain_mcp_adapter.persist_events(run_id, events)
    return {"persisted": True, "eventCount": len(events)}


@mcp.tool()
async def update_run_status(run_id: int, status: str, payload: dict[str, Any] | None = None) -> dict[str, Any]:
    """更新 Java 中的 Agent run 状态。"""
    await resume_domain_mcp_adapter.update_run_status(run_id, status, payload or {})
    return {"updated": True, "runId": run_id, "status": status}


if __name__ == "__main__":
    mcp.run()
