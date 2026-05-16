from typing import Any

from langchain_core.messages import BaseMessage


def estimate_text_tokens(text: str) -> int:
    """粗略估算 MiniMax token 数。

    MiniMax 没有项目内可用的精确 tokenizer，这里只用于判断是否提前摘要。
    中文按 1 字约 1 token，英文/数字/符号按 4 字符约 1 token。
    """
    if not text:
        return 0

    chinese_chars = sum(1 for char in text if "\u4e00" <= char <= "\u9fff")
    other_chars = len(text) - chinese_chars
    return chinese_chars + max(1, other_chars // 4)


def estimate_message_tokens(message: BaseMessage | dict[str, Any] | object) -> int:
    """估算单条消息 token，兼容 LangChain Message 和 dict。"""
    if isinstance(message, BaseMessage):
        content = message.content
    elif isinstance(message, dict):
        content = message.get("content")
    else:
        content = getattr(message, "content", "")

    return estimate_text_tokens(str(content or "")) + 8


def estimate_messages_tokens(messages: list[BaseMessage | dict[str, Any] | object]) -> int:
    """估算一组 messages 的 token 总量。"""
    return sum(estimate_message_tokens(message) for message in messages)
