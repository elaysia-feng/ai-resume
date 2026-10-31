from typing import Any

from langchain_core.messages import BaseMessage


def estimate_text_tokens(text: str) -> int:
    """按中英文字符比例粗略估算 token 数。

    该估算不依赖某一家模型的 tokenizer，只用于判断是否提前摘要。
    中文按 1 字约 1 token，英文/数字/符号按 4 字符约 1 token。

    Args:
        text: 需要估算的文本。

    Returns:
        粗略 token 数。
    """
    if not text:
        return 0

    chinese_chars = sum(1 for char in text if "\u4e00" <= char <= "\u9fff")
    other_chars = len(text) - chinese_chars
    return chinese_chars + max(1, other_chars // 4)


def estimate_message_tokens(message: BaseMessage | dict[str, Any] | object) -> int:
    """估算单条消息 token，兼容 LangChain Message 和 dict。

    Args:
        message: LangChain 消息、字典或含 content 属性的对象。

    Returns:
        消息内容 token 估算值及固定格式开销。
    """
    if isinstance(message, BaseMessage):
        content = message.content
    elif isinstance(message, dict):
        content = message.get("content")
    else:
        content = getattr(message, "content", "")

    return estimate_text_tokens(str(content or "")) + 8


def estimate_messages_tokens(messages: list[BaseMessage | dict[str, Any] | object]) -> int:
    """估算一组 messages 的 token 总量。

    Args:
        messages: 待估算的消息列表。

    Returns:
        消息 token 估算总量。
    """
    return sum(estimate_message_tokens(message) for message in messages)
