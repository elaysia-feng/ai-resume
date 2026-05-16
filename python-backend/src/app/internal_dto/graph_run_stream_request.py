from pydantic import BaseModel, Field


class GraphRunStreamRequest(BaseModel):
    """Java 调 Python 启动 Agent run 的请求。"""

    run_id: int = Field(..., alias="runId", description="Java 侧 ai_agent_run 主键")
    session_id: int = Field(..., alias="sessionId", description="AI 会话 ID")
    resume_id: int = Field(..., alias="resumeId", description="简历 ID")
    scene_code: str = Field(..., alias="sceneCode", description="场景码，例如 JD_CUSTOMIZE")
    user_input: str | None = Field(default=None, alias="userInput", description="用户原始输入")
    job_description: str | None = Field(default=None, alias="jobDescription", description="岗位 JD")
    target_section_id: int = Field(..., alias="targetSectionId", description="本次只允许 Agent 修改的目标模块 ID")
