"""Agent 专用异常。"""


class AgentError(Exception):
    """Agent 基础异常。"""


class AgentStateError(AgentError):
    """Agent state 不合法。"""


class AgentRouteError(AgentError):
    """Agent 路由结果不合法。"""


class AgentToolError(AgentError):
    """Agent 工具调用失败。"""


class AgentReviewError(AgentError):
    """Reviewer 审查失败。"""


class AgentCheckpointError(AgentError):
    """Checkpoint 读写失败。"""
