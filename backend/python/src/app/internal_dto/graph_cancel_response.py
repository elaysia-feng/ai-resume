from pydantic import BaseModel, Field


class GraphCancelResponse(BaseModel):
    """取消 run 的响应。"""

    run_id: int = Field(..., alias="runId", description="Java 侧 ai_agent_run 主键")
    cancelled: bool = Field(default=True, description="是否已取消")
