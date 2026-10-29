"""为同步 SqliteSaver 提供异步图执行所需的持久化接口。

和原来的 redis_checkpoint.py 同构：LangGraph 的同步 saver 不提供 async 方法，
而图是异步执行的，所以这里把阻塞 I/O 挪到线程池。

为什么用同步 SqliteSaver 而不是 AsyncSqliteSaver：
AsyncSqliteSaver.__init__ 里会调 asyncio.get_running_loop()，
必须在事件循环内构造；而 graph 是在模块导入时编译的（那时没有运行中的 loop）。
同步版没有这个约束，配合线程池就能在导入期安全构造。
"""

import asyncio
import sqlite3
from pathlib import Path
from typing import Any, AsyncIterator

from langgraph.checkpoint.sqlite import SqliteSaver


class AsyncCompatibleSqliteSaver(SqliteSaver):
    """复用 SqliteSaver 的表结构和序列化，把异步执行中的阻塞 I/O 移到线程池。"""

    @classmethod
    def open(cls, db_path: str) -> "AsyncCompatibleSqliteSaver":
        """打开（或创建）checkpoint 数据库并建表。

        Args:
            db_path: SQLite 文件路径，父目录不存在会自动创建。

        Returns:
            已完成 setup 的 saver 实例。
        """
        path = Path(db_path)
        if path.parent and str(path.parent) not in ("", "."):
            path.parent.mkdir(parents=True, exist_ok=True)
        # check_same_thread=False：checkpoint 会被线程池里的工作线程访问
        conn = sqlite3.connect(str(path), check_same_thread=False)
        saver = cls(conn)
        saver.setup()
        return saver

    async def aget_tuple(self, config):
        """异步读取 checkpoint。

        Args:
            config: 包含 thread_id 等信息的运行配置。

        Returns:
            checkpoint 元组，未命中时为 None。
        """
        return await asyncio.to_thread(self.get_tuple, config)

    async def aput(self, config, checkpoint, metadata, new_versions):
        """异步保存 checkpoint。

        Args:
            config: 当前运行配置。
            checkpoint: 待保存状态。
            metadata: checkpoint 元数据。
            new_versions: 本次更新的通道版本。

        Returns:
            包含新 checkpoint 标识的运行配置。
        """
        return await asyncio.to_thread(self.put, config, checkpoint, metadata, new_versions)

    async def aput_writes(self, config, writes, task_id, task_path=""):
        """异步保存节点的中间写入。

        Args:
            config: 当前运行配置。
            writes: 节点产生的通道写入。
            task_id: 节点任务标识。
            task_path: 节点任务路径。
        """
        await asyncio.to_thread(self.put_writes, config, writes, task_id, task_path)

    async def alist(
        self,
        config,
        *,
        filter: dict[str, Any] | None = None,
        before: Any | None = None,
        limit: int | None = None,
    ) -> AsyncIterator[Any]:
        """异步列举 checkpoint。

        Args:
            config: 运行配置，None 表示列举全部。
            filter: 可选的过滤条件。
            before: 只返回该 checkpoint 之前的结果。
            limit: 最多返回多少条。

        Yields:
            checkpoint 元组。
        """
        items = await asyncio.to_thread(
            lambda: list(self.list(config, filter=filter, before=before, limit=limit))
        )
        for item in items:
            yield item

    async def adelete_thread(self, thread_id: str) -> None:
        """异步删除某个 thread 的全部 checkpoint。

        Args:
            thread_id: 线程标识。
        """
        await asyncio.to_thread(self.delete_thread, thread_id)
