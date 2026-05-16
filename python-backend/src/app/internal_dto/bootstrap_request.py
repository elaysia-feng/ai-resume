from pydantic import BaseModel, Field


class BootstrapRequest(BaseModel):
    """Python 请求 Java 加载 Agent 上下文。"""

    run_id: int = Field(..., alias="runId", description="Java 侧 ai_agent_run 主键")
    session_id: int = Field(..., alias="sessionId", description="AI 会话 ID")
    resume_id: int = Field(..., alias="resumeId", description="简历 ID")
    scene_code: str = Field(..., alias="sceneCode", description="场景码，例如 JD_CUSTOMIZE")
    target_section_id: int = Field(..., alias="targetSectionId", description="本次只允许 Agent 修改的目标模块 ID")
