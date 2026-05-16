from pydantic import BaseModel, Field

from app.internal_dto.resume_snapshot import ResumeSnapshot


class InterviewBootstrapResponse(BaseModel):
    """Java 返回给 Python 的面试模拟上下文。"""

    run_id: int = Field(..., alias="runId", description="Java 侧 ai_agent_run 主键")
    session_id: int = Field(..., alias="sessionId", description="AI 会话 ID")
    resume_id: int | None = Field(default=None, alias="resumeId", description="简历 ID")
    resume: ResumeSnapshot = Field(..., description="简历快照")
    job_description: str | None = Field(default=None, alias="jobDescription", description="当前会话复用的目标岗位 JD")
    summary: str | None = Field(default=None, description="当前会话长期记忆摘要")
