"""为现有同步 Redis 生命周期提供异步图执行所需的持久化接口。"""

import asyncio

from langgraph.checkpoint.redis import RedisSaver


class AsyncCompatibleRedisSaver(RedisSaver):
    """复用 RedisSaver 的连接和索引配置，将异步执行中的阻塞 I/O 移到线程池。"""

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
