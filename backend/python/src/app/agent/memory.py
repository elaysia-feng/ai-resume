from typing import Any

from langchain_core.messages import AIMessage, BaseMessage, HumanMessage, RemoveMessage, SystemMessage

from src.app.agent.state import ResumeAgentState
from src.app.agent.token_estimator import estimate_messages_tokens
from src.app.config.settings import get_settings
from src.app.service.agent_factory import agent_factory

SUMMARY_PROMPT = """请总结上面的简历 Agent 对话上下文。
要求：
1. 只保留后续改写简历需要的信息
2. 保留用户目标、岗位要求、已确认事实、禁忌和待处理事项
3. 不要编造简历中不存在的经历、公司、项目、时间和指标
4. 用简洁中文输出"""


def build_memory_messages(state: ResumeAgentState) -> list[BaseMessage]:
    """构造传给节点 Agent 的共享记忆消息。

    summary 保存被压缩掉的旧上下文，messages 保存最近几轮原始消息。
    节点 Agent 调模型时同时带上两者，既能保留长期上下文，又不会让消息无限增长。
    这里只负责“读取并拼装模型输入”，不会写回 LangGraph state。
    """
    result: list[BaseMessage] = []
    summary = state.get("summary")
    if summary:
        # 摘要放在 SystemMessage 里，相当于给后续节点一个长期背景说明。
        result.append(SystemMessage(content=f"历史对话摘要：\n{summary}"))
    # 最近消息保留原始 Human/AI/SystemMessage，避免摘要丢失刚发生的细节。
    result.extend(state.get("messages") or [])
    return result


def build_initial_messages(state: ResumeAgentState) -> list[BaseMessage]:
    """把 Java 历史消息和本轮输入放入 MessagesState。

    只有 graph 输入里没有 messages 时才使用，避免 continue 或 Studio Chat 重复追加。
    """
    # Java 保存的历史消息是普通 dict，这里先转成 LangChain 消息对象。
    messages = _history_to_messages(state.get("history_messages", []), state.get("run_id", 0))
    if state.get("user_input"):
        # 本轮用户输入也放进 messages，后续每个节点都能通过 build_memory_messages 看到。
        messages.append(HumanMessage(content=f"用户输入：{state['user_input']}", id=_message_id(state, "user-input")))
    if state.get("job_description"):
        messages.append(HumanMessage(content=f"目标岗位 JD：{state['job_description']}", id=_message_id(state, "job-description")))
    return messages


async def compact_memory_for_model(
    state: ResumeAgentState,
    model_messages: list[BaseMessage],
) -> tuple[ResumeAgentState, ResumeAgentState]:
    """模型调用前的上下文守卫。

    这里检查的是“本次真实要发给模型的 messages”，不是图上某条边。
    如果 prompt 接近 MiniMax 上下文上限，就先压缩 state["messages"]，
    再让当前节点用压缩后的 state 重新构造 prompt 并继续执行。
    """
    settings = get_settings()
    prompt_tokens = estimate_messages_tokens(model_messages)
    trigger_tokens = int(settings.agent_model_context_length * settings.agent_summary_trigger_context_ratio)
    if prompt_tokens < trigger_tokens:
        return {}, state

    source_messages = list(state.get("messages") or [])
    if not source_messages:
        # 超长内容如果来自简历快照/JD/patch，本轮没有可删除的历史消息，只能继续交给模型或由模型报错。
        return {}, state

    summary = await summarize_messages(source_messages, state.get("summary", ""))
    if not summary:
        return {}, state

    keep_count = max(settings.agent_summary_keep_recent_messages, 0)
    recent_messages = source_messages[-keep_count:] if keep_count else []
    updates: ResumeAgentState = {
        "summary": summary,
        "memory_last_summary_token_count": estimate_messages_tokens(source_messages),
    }
    delete_messages = build_delete_messages(source_messages, keep_count)
    if delete_messages:
        updates["messages"] = delete_messages

    compacted_state = dict(state)
    compacted_state["summary"] = summary
    compacted_state["messages"] = recent_messages
    return updates, compacted_state


async def summarize_messages(messages: list[BaseMessage], existing_summary: str) -> str:
    """调用 ChatModel 生成新的长期摘要。"""
    if existing_summary:
        # 已有摘要时不是重新总结全部，而是把旧摘要和新增消息合并成更新版摘要。
        summary_message = (
            f"当前已有摘要：\n{existing_summary}\n\n"
            "请结合上面的新增消息，更新为一份完整摘要。"
        )
    else:
        summary_message = "请为上面的对话创建一份长期记忆摘要。"

    try:
        model = agent_factory.create_chat_model()
        response = await model.ainvoke([*messages, HumanMessage(content=f"{SUMMARY_PROMPT}\n\n{summary_message}")])
    except Exception:
        return ""

    content = getattr(response, "content", "")
    if isinstance(content, str):
        return content.strip()
    return str(content).strip()


def build_delete_messages(messages: list[BaseMessage], keep_recent_count: int) -> list[RemoveMessage]:
    """生成 RemoveMessage，让 MessagesState reducer 删除旧消息。"""
    keep_count = max(keep_recent_count, 0)
    messages_to_delete = messages if keep_count == 0 else messages[:-keep_count]
    return [RemoveMessage(id=message.id) for message in messages_to_delete if message.id]


def _history_to_messages(history_messages: list[dict[str, Any]], run_id: int) -> list[BaseMessage]:
    """把 Java/数据库里取出来的历史消息 dict，转换成 LangChain 能识别的 BaseMessage 对象列表。"""

    result: list[BaseMessage] = []
    for index, item in enumerate(history_messages):
        # enumerate 同时给出下标和消息内容，下标用于生成稳定 message id。
        role = str(item.get("role") or "").strip().lower()
        content = str(item.get("content") or "").strip()
        if not content:
            continue

        message_id = f"run-{run_id}-history-{index}"
        if role == "user":
            result.append(HumanMessage(content=content, id=message_id))
        elif role == "assistant":
            result.append(AIMessage(content=content, id=message_id))
        elif role == "system":
            result.append(SystemMessage(content=content, id=message_id))
    return result


def _message_id(state: ResumeAgentState, suffix: str) -> str:
    """生成当前 run 内稳定的消息 ID，方便 RemoveMessage 后续按 id 删除旧消息。"""
    return f"run-{state.get('run_id', 0)}-{suffix}"
