from pydantic import BaseModel, Field


class HistoryMessageSnapshot(BaseModel):
    """Java 返回的历史消息快照。"""

    role: str = Field(..., description="消息角色")
    content: str = Field(..., description="消息内容")
    content_type: str | None = Field(default=None, alias="contentType", description="消息内容类型")
    tool_name: str | None = Field(default=None, alias="toolName", description="工具消息对应的工具名称")
    extra_json: str | None = Field(default=None, alias="extraJson", description="扩展信息 JSON 字符串")
