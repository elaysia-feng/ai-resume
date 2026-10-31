"""Java 代理到 Python 的工作区聊天请求。"""

from pydantic import BaseModel, ConfigDict, Field


class WorkspaceChatMessage(BaseModel):
    """保留 Java 消息表中的通用历史字段。

    Attributes:
        role: 消息角色。
        content: 文本或历史工具内容块 JSON。
        content_type: Java 消息内容类型。
        tool_name: 工具结果对应的工具名。
    """

    model_config = ConfigDict(populate_by_name=True, extra="ignore")

    role: str
    content: str = ""
    content_type: str = Field(default="TEXT", alias="contentType")
    tool_name: str | None = Field(default=None, alias="toolName")


class WorkspaceChatRequest(BaseModel):
    """工作区聊天上下文；用户身份和工作区权限已由 Java 校验。

    Attributes:
        workspace_root: Java 为当前用户解析的工作区根目录。
        messages: 已保存的有序聊天历史。
    """

    model_config = ConfigDict(populate_by_name=True, extra="ignore")

    workspace_root: str | None = Field(default=None, alias="workspaceRoot")
    messages: list[WorkspaceChatMessage] = Field(default_factory=list)
