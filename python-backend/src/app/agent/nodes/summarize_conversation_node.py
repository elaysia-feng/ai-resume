from src.app.agent.constants import AgentStage
from src.app.agent.memory import build_delete_messages, summarize_messages
from src.app.agent.routing import should_summarize_memory
from src.app.agent.state import ResumeAgentState
from src.app.agent.token_estimator import estimate_messages_tokens
from src.app.config.settings import get_settings


async def summarize_conversation_node(state: ResumeAgentState) -> ResumeAgentState:
    """摘要记忆节点：压缩旧 messages，保留 summary 和最近消息。"""
    messages = list(state.get("messages") or [])
    settings = get_settings()
    token_count = estimate_messages_tokens(messages)
    result: ResumeAgentState = {
        "current_stage": AgentStage.MEMORY_SUMMARY,
        "memory_last_summary_token_count": token_count,
    }

    if not should_summarize_memory(state):
        # 正常图路由不会在未达阈值时进入这里；这个判断是为了 Studio 手动调试时兜底。
        return result

    summary = await summarize_messages(messages, state.get("summary", ""))
    if not summary:
        return result

    result["summary"] = summary
    delete_messages = build_delete_messages(messages, settings.agent_summary_keep_recent_messages)
    if delete_messages:
        # RemoveMessage 会让 MessagesState reducer 删除旧消息，只保留最近几轮原始消息。
        result["messages"] = delete_messages
    return result
