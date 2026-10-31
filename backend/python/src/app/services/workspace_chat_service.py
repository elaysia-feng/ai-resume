"""统一 ChatModel 工作区对话与本地只读文件工具。"""

import json
import logging
import uuid
from collections import deque
from collections.abc import AsyncIterator
from pathlib import Path
from typing import Any

from langchain_core.messages import (
    AIMessage,
    AIMessageChunk,
    BaseMessage,
    HumanMessage,
    SystemMessage,
    ToolMessage,
    message_chunk_to_message,
)
from langchain_core.tools import StructuredTool

from app.core.llm import agent_factory
from app.schemas.internal.workspace_chat_request import WorkspaceChatMessage, WorkspaceChatRequest

logger = logging.getLogger("uvicorn.error")

EXCLUDED_DIR_NAMES = {"node_modules", "target", ".git", ".idea", ".venv", "__pycache__", "dist", "build"}
DEFAULT_MAX_FILE_BYTES = 200 * 1024
MAX_FILE_BYTES = 1024 * 1024
MAX_CONTENT_SCAN_BYTES = 64 * 1024
MAX_TOOL_ROUNDS = 8


class WorkspaceChatService:
    """执行工作区对话；模型凭据与 adapter 由 AgentFactory 统一管理。"""

    async def stream(self, request: WorkspaceChatRequest) -> AsyncIterator[str]:
        """按模型生成、工具执行的顺序产生前端兼容的 SSE 事件。

        Args:
            request: Java 校验会话后传入的工作区上下文。

        Yields:
            JSON 编码的 text、tool、done 或 error 事件。
        """
        try:
            # 1. 创建统一 ChatModel，绑定工作区只读工具并还原历史消息。
            model = agent_factory.create_chat_model()
            tools = self._build_tools(request.workspace_root)
            model_with_tools = model.bind_tools(tools)
            messages = self._to_chat_messages(request.messages)
            messages.insert(0, SystemMessage(content=self._system_prompt(request.workspace_root)))

            for round_index in range(MAX_TOOL_ROUNDS + 1):
                # 2. 流式读取当前轮输出；文本立即转发，工具调用留到流结束后处理。
                assistant_chunk: AIMessageChunk | None = None
                async for chunk in model_with_tools.astream(messages):
                    if isinstance(chunk, AIMessageChunk):
                        assistant_chunk = chunk if assistant_chunk is None else assistant_chunk + chunk
                    for text in self._text_parts(getattr(chunk, "content", "")):
                        if text:
                            yield self._sse("text_delta", {"text": text})

                if assistant_chunk is None:
                    yield self._sse("error", {"message": "模型没有返回内容，请检查所选集成是否支持流式对话"})
                    return

                assistant_message = message_chunk_to_message(assistant_chunk)
                tool_calls = assistant_message.tool_calls or []
                if not tool_calls:
                    finish_reason = assistant_message.response_metadata.get("finish_reason", "stop")
                    yield self._sse("done", {"finishReason": finish_reason or "stop"})
                    return

                if round_index >= MAX_TOOL_ROUNDS:
                    yield self._sse("error", {"message": "本轮工具调用次数过多，请缩小问题范围后重试"})
                    return

                normalized_calls = [self._normalize_tool_call(call) for call in tool_calls]
                history_content = json.dumps(
                    {
                        "type": "workspace_tool_calls",
                        "text": self._content_text(assistant_message.content),
                        "calls": normalized_calls,
                    },
                    ensure_ascii=False,
                )
                yield self._sse("tool_calls_complete", {"historyContent": history_content})
                messages.append(assistant_message)
                tool_map = {tool.name: tool for tool in tools}

                # 3. 执行本轮工具调用，并把完整结果放回下一轮模型上下文。
                for call in normalized_calls:
                    tool_id = call["id"]
                    name = call["name"]
                    arguments = call["input"]
                    yield self._sse("tool_call", {"id": tool_id, "name": name, "input": arguments})
                    ok, summary, content = self._execute_tool(tool_map, name, arguments)
                    history_content = json.dumps(
                        {
                            "type": "workspace_tool_result",
                            "id": tool_id,
                            "name": name,
                            "ok": ok,
                            "summary": summary,
                            "content": content,
                        },
                        ensure_ascii=False,
                    )
                    yield self._sse(
                        "tool_result",
                        {
                            "id": tool_id,
                            "name": name,
                            "ok": ok,
                            "summary": summary,
                            "content": content,
                            "historyContent": history_content,
                        },
                    )
                    messages.append(ToolMessage(
                        content=content,
                        tool_call_id=tool_id,
                        name=name,
                        status="success" if ok else "error",
                    ))
        except Exception as error:
            # 不回传或记录供应商异常原文，避免意外泄露鉴权头或 URL 中的凭据。
            logger.warning("工作区聊天失败, errorType=%s", type(error).__name__)
            yield self._sse(
                "error",
                {"message": f"模型调用失败（{type(error).__name__}），请检查端点、凭据和协议兼容性"},
            )

    def _build_tools(self, workspace_root: str | None) -> list[StructuredTool]:
        # 1. 将工作区路径限制在闭包中，只向模型公开文件操作参数。
        def read_local_file(path: str, maxBytes: int = DEFAULT_MAX_FILE_BYTES) -> str:
            target = self._resolve_existing(path, workspace_root)
            if target.is_dir():
                raise ValueError("目标是一个目录，请改用 list_dir")
            limit = maxBytes if 0 < maxBytes <= MAX_FILE_BYTES else DEFAULT_MAX_FILE_BYTES
            try:
                with target.open("rb") as file:
                    data = file.read(limit + 1)
            except OSError as error:
                raise ValueError("读取文件失败") from error
            truncated = len(data) > limit
            content_bytes = data[:limit] if truncated else data
            if self._is_binary(content_bytes):
                raise ValueError("不支持读取二进制文件")
            content = content_bytes.decode("utf-8", errors="replace")
            summary = f"读取了 {len(content_bytes)} 字节"
            if truncated:
                summary += f"（内容超过 {limit} 字节，已截断）"
                content += "\n...[内容已截断]"
            return self._tool_result(summary, content)

        def list_dir(path: str = "", recursive: bool = False, maxEntries: int = 500) -> str:
            root = self._resolve_existing(path, workspace_root)
            if not root.is_dir():
                raise ValueError("目标不是目录")
            limit = maxEntries if 0 < maxEntries <= 500 else 500
            entries: list[str] = []
            if recursive:
                pending: list[Path] = [root]
                while pending and len(entries) < limit:
                    current = pending.pop()
                    try:
                        children = sorted(current.iterdir(), key=lambda item: item.name.casefold())
                    except OSError:
                        continue
                    for child in children:
                        if len(entries) >= limit:
                            break
                        if child.is_symlink():
                            continue
                        if child.is_dir():
                            if self._is_excluded(child.name):
                                continue
                            entries.append(f"[DIR ] {child.name}")
                            if len(child.relative_to(root).parts) < 8:
                                pending.append(child)
                        else:
                            entries.append(self._describe_file(child))
            else:
                try:
                    children = sorted(root.iterdir(), key=lambda item: item.name.casefold())
                except OSError as error:
                    raise ValueError("读取目录失败") from error
                for child in children:
                    if child.is_symlink():
                        continue
                    if child.is_dir() and self._is_excluded(child.name):
                        continue
                    if len(entries) >= limit:
                        break
                    entries.append(self._describe_file(child))
            body = "\n".join(entries) if entries else "(目录为空)"
            return self._tool_result(f"列出 {len(entries)} 个条目", body)

        def search_in_files(query: str, path: str = "", maxResults: int = 100) -> str:
            if not query or not query.strip():
                raise ValueError("搜索关键字不能为空")
            limit = maxResults if 0 < maxResults <= 100 else 100
            start = self._resolve_existing(path, workspace_root)
            keyword = query.strip().casefold()
            if start.is_file():
                hits = self._match_file(start, workspace_root, keyword, limit)
            elif start.is_dir():
                all_files = self._collect_files(start, 300_000)
                hits = []
                for file in all_files:
                    if len(hits) >= limit:
                        break
                    if keyword in file.name.casefold():
                        hits.append(f"{self._relative(file, workspace_root)}（文件名匹配）")
                for file in all_files[:5000]:
                    if len(hits) >= limit:
                        break
                    hits.extend(self._match_file_content(file, workspace_root, keyword, limit - len(hits)))
            else:
                raise ValueError("搜索路径必须是目录或文件")
            body = "\n".join(hits) if hits else "(没有找到匹配内容)"
            return self._tool_result(f"命中 {len(hits)} 条", body)

        def read_workspace_tree(path: str = "", depth: int = 2) -> str:
            level_limit = depth if 0 < depth <= 6 else 2
            root = self._resolve_existing(path, workspace_root)
            if not root.is_dir():
                raise ValueError("目标不是目录")
            lines = [self._relative(root, workspace_root)]

            def append_children(directory: Path, level: int) -> None:
                if level > level_limit or len(lines) >= 500:
                    return
                try:
                    children = sorted(directory.iterdir(), key=lambda item: item.name.casefold())
                except OSError:
                    return
                for child in children:
                    if len(lines) >= 500:
                        return
                    if child.is_symlink():
                        continue
                    name = child.name
                    if child.is_dir():
                        if self._is_excluded(name):
                            continue
                        lines.append(f"{'  ' * (level - 1)}{name}/")
                        append_children(child, level + 1)
                    else:
                        lines.append(f"{'  ' * (level - 1)}{name}")

            append_children(root, 1)
            return self._tool_result(f"目录树共 {len(lines)} 项", "\n".join(lines))

        # 2. 通过 StructuredTool 生成 provider 无关的工具描述。
        return [
            StructuredTool.from_function(
                read_local_file,
                name="read_local_file",
                description="读取工作区内的文本文件。路径必须位于工作区内；二进制文件会被拒绝。",
            ),
            StructuredTool.from_function(
                list_dir,
                name="list_dir",
                description="列出工作区内的目录内容，默认非递归，最多 500 条。",
            ),
            StructuredTool.from_function(
                search_in_files,
                name="search_in_files",
                description="在工作区内按文件名或文件内容搜索关键字，跳过 node_modules 等目录。",
            ),
            StructuredTool.from_function(
                read_workspace_tree,
                name="read_workspace_tree",
                description="列出工作区目录树，默认深度 2，最多 500 项。",
            ),
        ]

    def _to_chat_messages(self, history: list[WorkspaceChatMessage]) -> list[BaseMessage]:
        # 1. 读取角色与通用或旧版 JSON 工具历史。
        messages: list[BaseMessage] = []
        for item in history:
            role = item.role.upper()
            stored_content = self._json_content(item.content) if item.content_type.upper() == "JSON" else None
            if (
                role == "ASSISTANT"
                and isinstance(stored_content, dict)
                and stored_content.get("type") == "workspace_tool_calls"
            ):
                calls = [
                    {
                        "name": call.get("name", ""),
                        "args": call.get("input", {}),
                        "id": call.get("id") or f"stored-tool-{len(messages)}-{index}",
                        "type": "tool_call",
                    }
                    for index, call in enumerate(stored_content.get("calls") or [])
                    if isinstance(call, dict)
                ]
                messages.append(AIMessage(content=stored_content.get("text", ""), tool_calls=calls))
            elif role == "ASSISTANT" and isinstance(stored_content, list):
                # 2. 将旧版工具调用块还原成 LangChain AIMessage。
                calls = [
                    {
                        "name": block.get("name", ""),
                        "args": block.get("input", {}),
                        "id": block.get("id") or f"legacy-tool-{len(messages)}-{index}",
                        "type": "tool_call",
                    }
                    for index, block in enumerate(stored_content)
                    if isinstance(block, dict) and block.get("type") == "tool_use"
                ]
                text = "".join(
                    str(block.get("text", ""))
                    for block in stored_content
                    if isinstance(block, dict) and block.get("type") == "text"
                )
                if calls:
                    messages.append(AIMessage(content=text, tool_calls=calls))
                elif text:
                    messages.append(AIMessage(content=text))
                else:
                    messages.append(AIMessage(content=item.content))
            elif (
                role == "TOOL"
                and isinstance(stored_content, dict)
                and stored_content.get("type") == "workspace_tool_result"
            ):
                messages.append(ToolMessage(
                    content=str(stored_content.get("content", "")),
                    tool_call_id=str(stored_content.get("id") or f"stored-result-{len(messages)}"),
                    name=str(stored_content.get("name") or item.tool_name or "tool"),
                    status="success" if stored_content.get("ok", True) else "error",
                ))
            elif role == "TOOL" and isinstance(stored_content, list):
                # 3. 将旧版工具结果块还原成带调用 ID 的 ToolMessage。
                for index, block in enumerate(stored_content):
                    if not isinstance(block, dict) or block.get("type") != "tool_result":
                        continue
                    content = self._content_text(block.get("content", ""))
                    messages.append(ToolMessage(
                        content=content,
                        tool_call_id=str(block.get("tool_use_id") or f"legacy-result-{len(messages)}-{index}"),
                        name=item.tool_name,
                        status="error" if block.get("is_error") else "success",
                    ))
            elif role == "USER":
                messages.append(HumanMessage(content=item.content))
            elif role == "ASSISTANT":
                messages.append(AIMessage(content=item.content))
        return messages

    def _system_prompt(self, workspace_root: str | None) -> str:
        prompt = "你是 AI Resume Forge 的桌面助手，可以阅读和分析用户工作区内的文件。\n"
        if not workspace_root or not workspace_root.strip():
            prompt += "用户尚未选择工作区目录，文件工具会提示这一点；请引导用户先选择工作区。\n"
        else:
            prompt += f"工作区根目录：{workspace_root}\n所有文件工具只能访问该目录内路径，越界访问会被拒绝。\n"
        return prompt + "需要文件内容时优先调用工具读取，不要凭空猜测。"

    def _execute_tool(
        self,
        tools: dict[str, StructuredTool],
        name: str,
        arguments: dict[str, Any],
    ) -> tuple[bool, str, str]:
        tool = tools.get(name)
        if tool is None:
            return False, "未知工具", f"未知工具：{name}"
        try:
            raw_result = tool.invoke(arguments)
            result = json.loads(raw_result) if isinstance(raw_result, str) else raw_result
            return True, str(result.get("summary", "工具执行完成")), str(result.get("content", ""))
        except Exception as error:
            return False, "工具执行失败", str(error) or "工具执行失败"

    def _normalize_tool_call(self, call: dict[str, Any]) -> dict[str, Any]:
        arguments = call.get("args", {})
        if not isinstance(arguments, dict):
            arguments = {}
        return {
            "id": str(call.get("id") or uuid.uuid4()),
            "name": str(call.get("name") or ""),
            "input": arguments,
        }

    def _resolve_existing(self, raw_path: str | None, workspace_root: str | None) -> Path:
        # 1. 要求工作区根目录存在，并拒绝显式父目录回退。
        if not workspace_root or not workspace_root.strip():
            raise ValueError("尚未选择工作区目录")
        try:
            root = Path(workspace_root).expanduser().resolve(strict=True)
        except (OSError, RuntimeError) as error:
            raise ValueError("工作区目录不存在，请重新选择工作区目录") from error
        if not root.is_dir():
            raise ValueError("工作区目录不存在，请重新选择工作区目录")
        raw = (raw_path or "").strip()
        if any(part == ".." for part in raw.replace("\\", "/").split("/")):
            raise ValueError("路径不在工作区内")
        # 2. 解析真实路径后再次确认它仍位于工作区内，阻止符号链接越界。
        candidate = Path(raw).expanduser() if raw else root
        if not candidate.is_absolute():
            candidate = root / candidate
        try:
            resolved = candidate.resolve(strict=True)
        except FileNotFoundError as error:
            raise ValueError("文件或目录不存在") from error
        except (OSError, RuntimeError) as error:
            raise ValueError("无法访问该路径") from error
        try:
            resolved.relative_to(root)
        except ValueError as error:
            raise ValueError("路径不在工作区内") from error
        return resolved

    def _collect_files(self, start: Path, max_files: int) -> list[Path]:
        # 1. 初始化广度优先队列与结果列表。
        files: list[Path] = []
        pending: deque[Path] = deque([start])
        while pending and len(files) < max_files:
            # 2. 遍历当前目录，只将安全子目录加入待处理队列。
            current = pending.popleft()
            try:
                children = list(current.iterdir())
            except OSError:
                continue
            for child in children:
                if len(files) >= max_files:
                    break
                if child.is_symlink():
                    continue
                if child.is_dir():
                    if not self._is_excluded(child.name):
                        pending.append(child)
                else:
                    files.append(child)
        return files

    def _match_file(self, file: Path, workspace_root: str | None, keyword: str, limit: int) -> list[str]:
        hits = []
        if keyword in file.name.casefold():
            hits.append(f"{self._relative(file, workspace_root)}（文件名匹配）")
        hits.extend(self._match_file_content(file, workspace_root, keyword, max(0, limit - len(hits))))
        return hits

    def _match_file_content(
        self,
        file: Path,
        workspace_root: str | None,
        keyword: str,
        remaining: int,
    ) -> list[str]:
        # 1. 检查剩余命中数，避免超出请求上限。
        if remaining <= 0:
            return []
        # 2. 只扫描文件头部的有限字节。
        try:
            with file.open("rb") as stream:
                data = stream.read(MAX_CONTENT_SCAN_BYTES)
        except OSError:
            return []
        # 3. 跳过二进制内容，再逐行匹配并限制结果长度。
        if self._is_binary(data):
            return []
        content = data.decode("utf-8", errors="replace")
        hits = []
        for line_number, line in enumerate(content.splitlines(), start=1):
            if keyword in line.casefold():
                trimmed = line.strip()
                if len(trimmed) > 200:
                    trimmed = trimmed[:200] + "..."
                hits.append(f"{self._relative(file, workspace_root)}:{line_number}: {trimmed}")
                if len(hits) >= min(5, remaining):
                    break
        return hits

    def _describe_file(self, path: Path) -> str:
        if path.is_dir():
            return f"[DIR ] {path.name}"
        try:
            size = path.stat().st_size
        except OSError:
            return f"[FILE] {path.name}"
        return f"[FILE] {path.name}  ({size} 字节)"

    def _relative(self, path: Path, workspace_root: str | None) -> str:
        if not workspace_root:
            return path.name
        try:
            return str(path.relative_to(Path(workspace_root).expanduser().resolve()))
        except (OSError, ValueError):
            return path.name

    def _is_binary(self, data: bytes) -> bool:
        sample = data[:8192]
        if not sample:
            return False
        controls = sum(value == 0 or value < 0x09 or 0x0D < value < 0x20 for value in sample)
        return controls / len(sample) > 0.1

    def _is_excluded(self, name: str) -> bool:
        return name.casefold() in EXCLUDED_DIR_NAMES

    def _tool_result(self, summary: str, content: str) -> str:
        return json.dumps({"summary": summary, "content": content}, ensure_ascii=False)

    def _json_content(self, content: str) -> Any | None:
        try:
            value = json.loads(content)
        except (json.JSONDecodeError, TypeError):
            return None
        return value

    def _text_parts(self, content: Any) -> list[str]:
        if isinstance(content, str):
            return [content]
        if isinstance(content, list):
            return [
                str(part.get("text", ""))
                for part in content
                if isinstance(part, dict) and part.get("type") in {"text", "output_text"} and part.get("text")
            ]
        return []

    def _content_text(self, content: Any) -> str:
        if isinstance(content, str):
            return content
        if isinstance(content, list):
            return "".join(self._text_parts(content))
        return json.dumps(content, ensure_ascii=False)

    def _sse(self, event: str, data: dict[str, Any]) -> str:
        return f"event: {event}\ndata: {json.dumps(data, ensure_ascii=False)}\n\n"


workspace_chat_service = WorkspaceChatService()
