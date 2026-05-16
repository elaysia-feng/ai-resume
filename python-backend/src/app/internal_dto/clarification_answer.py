from pydantic import BaseModel, Field


class ClarificationAnswer(BaseModel):
    """用户对 Agent 追问的回答。"""

    field_key: str = Field(..., alias="fieldKey", description="追问字段标识")
    value: str = Field(..., min_length=1, description="用户回答内容")
