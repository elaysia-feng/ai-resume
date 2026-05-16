from pydantic import BaseModel, Field

from src.app.internal_dto.resume_section_snapshot import ResumeSectionSnapshot


class ResumeSnapshot(BaseModel):
    """简历快照。"""

    id: int = Field(..., description="简历 ID")
    title: str | None = Field(default=None, description="简历标题")
    template: str | None = Field(default=None, description="简历模板编码")
    sections: list[ResumeSectionSnapshot] = Field(default_factory=list, description="简历模块快照列表")
