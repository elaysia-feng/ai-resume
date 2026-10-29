"""LangGraph Studio 标准入口。

这个文件保持很薄，只负责把官方模板期望的 `src/agent/graph.py:graph`
转发到项目真实实现 `app.graphs.resume_agent.graph`。
"""

from app.graphs.resume_agent.graph import AgentGraphService, build_graph

graph = build_graph()

__all__ = ["AgentGraphService", "build_graph", "graph"]
