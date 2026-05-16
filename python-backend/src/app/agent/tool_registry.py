from collections.abc import Callable
from typing import Any


class ToolRegistry:
    """Agent 工具注册表骨架。"""

    def __init__(self) -> None:
        self._tools: dict[str, Callable[..., Any]] = {}

    def register(self, name: str, tool: Callable[..., Any]) -> None:
        """注册工具。"""
        self._tools[name] = tool

    def get(self, name: str) -> Callable[..., Any]:
        """获取工具。"""
        if name not in self._tools:
            raise KeyError(f"工具未注册: {name}")
        return self._tools[name]

    def list_names(self) -> list[str]:
        """列出已注册工具名称。"""
        return list(self._tools.keys())


tool_registry = ToolRegistry()
