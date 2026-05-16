from typing import Any

from pydantic import BaseModel, Field

from src.app.internal_dto.bootstrap_constraints import BootstrapConstraints
from src.app.internal_dto.history_message_snapshot import HistoryMessageSnapshot
from src.app.internal_dto.resume_snapshot import ResumeSnapshot


class BootstrapResponse(BaseModel):
    """Java 返回给 Python 的 Agent 上下文。"""

    run_id: int = Field(..., alias="runId", description="Java 侧 ai_agent_run 主键")
    session_id: int = Field(..., alias="sessionId", description="AI 会话 ID")
    resume: ResumeSnapshot = Field(..., description="简历快照")
    job_description: str | None = Field(default=None, alias="jobDescription", description="当前会话复用的目标岗位 JD")
    summary: str | None = Field(default=None, description="当前会话长期记忆摘要")
    schemas: dict[str, dict[str, Any]] = Field(default_factory=dict, description="简历模块 schema 映射")
    editable_section_ids: list[int] = Field(default_factory=list, alias="editableSectionIds", description="本次允许编辑的模块 ID")
    constraints: BootstrapConstraints = Field(default_factory=BootstrapConstraints, description="本次 Agent 运行约束")
    # latest_version: dict[str, Any] | None = Field(default=None, alias="latestVersion", description="简历最新版本信息")
