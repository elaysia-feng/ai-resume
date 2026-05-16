from src.app.agent.constants import AgentRoute
from src.app.agent.state import ResumeAgentState
from src.app.agent.token_estimator import estimate_messages_tokens
from src.app.config.settings import get_settings

MEMORY_ROUTE_SUMMARIZE = "summarize_conversation"


def decide_supervisor_route(state: ResumeAgentState) -> str:
    """根据 Supervisor Agent 输出决定下一节点。"""
    decision = state.get("route_decision", {})
    if decision.get("clarificationNeeded"):
        # 信息不足时先进入 clarifier，让用户补充 JD 或关键事实。
        return AgentRoute.CLARIFIER
    # nextNode 由 supervisor_node 写入；为空或异常时返回 failed，避免图走到未知节点。
    return normalize_route(decision.get("nextNode")) or AgentRoute.FAILED


def decide_memory_route(state: ResumeAgentState) -> str:
    """根据消息数量和上下文占比决定是否进入摘要节点。"""
    if should_summarize_memory(state):
        return MEMORY_ROUTE_SUMMARIZE
    return "supervisor"


def should_summarize_memory(state: ResumeAgentState) -> bool:
    """判断当前 messages 是否已经需要压缩。"""
    messages = state.get("messages") or []
    if not messages:
        return False

    settings = get_settings()
    token_count = estimate_messages_tokens(messages)

    if len(messages) >= settings.agent_summary_trigger_message_count:
        # 只有上下文消息达到阈值时才进入摘要节点，避免每次 run 都经过 summary。
        return token_count != state.get("memory_last_summary_token_count")

    trigger_tokens = int(settings.agent_model_context_length * settings.agent_summary_trigger_context_ratio)
    if token_count >= trigger_tokens:
        # MiniMax 上下文接近上限时先摘要，摘要完成后图会继续进入 supervisor。
        return token_count != state.get("memory_last_summary_token_count")
    return False


def decide_review_route(state: ResumeAgentState) -> str:
    """根据 Reviewer Agent 输出决定下一节点。"""
    if state.get("review_passed"):
        # 审查通过后不直接写简历，而是进入审批打包，等待用户确认 patch。
        return AgentRoute.APPROVAL_PACKAGER

    max_retry = get_settings().agent_review_max_retry
    if state.get("review_retry_count", 0) <= max_retry:
        # 审查不通过但还没超过重试次数，退回 rewriter 重新生成 patch。
        return AgentRoute.REWRITER
    # 多次重写仍不通过时结束本轮，避免无限循环。
    return AgentRoute.FAILED


def normalize_route(route: str | None) -> str:
    """规范化路由结果。"""
    if not route:
        return AgentRoute.FAILED
    return route.strip().lower()
