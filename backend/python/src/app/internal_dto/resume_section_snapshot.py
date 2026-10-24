from typing import Any

from pydantic import BaseModel, Field


class ResumeSectionSnapshot(BaseModel):
    """简历模块快照。"""

    id: int = Field(..., description="简历模块 ID")
    resume_id: int = Field(..., alias="resumeId", description="简历 ID")
    section_code: str = Field(..., alias="sectionCode", description="简历模块编码")
    section_title: str = Field(..., alias="sectionTitle", description="简历模块标题")
    section_type: str | None = Field(default=None, alias="sectionType", description="简历模块类型")
    schema_type: str | None = Field(default=None, alias="schemaType", description="模块内容 schema 类型")
    content_json: Any = Field(default_factory=dict, alias="contentJson", description="模块内容 JSON")
    visible: bool | None = Field(default=None, description="模块是否可见")
    sort_order: int | None = Field(default=None, alias="sortOrder", description="模块排序值")
