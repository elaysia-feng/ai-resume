from pydantic import BaseModel, Field as PydanticField


class HistoryMessage(BaseModel):
    """历史对话消息对象。"""

    role: str = PydanticField(..., min_length=1, description="消息角色，例如 system、user、assistant、tool")
    content: str = PydanticField(..., min_length=1, description="消息内容")
    contentType: str | None = PydanticField(default=None, description="消息内容类型，可选，例如 TEXT、JSON")
    toolName: str | None = PydanticField(default=None, description="工具名称，tool 消息时可选")
    extraJson: str | None = PydanticField(default=None, description="扩展信息 JSON 字符串，可选")


class HealthResponse(BaseModel):
    """健康检查响应对象。"""

    status: str = PydanticField(..., description="服务健康状态")
