import asyncio

from src.app.agent.types import ReferenceChunk
from src.app.service.embedding_service import embedding_service
from src.app.service.vector_store_service import vector_store_service


async def search_reference_chunks_tool(
    query: str,
    section_code: str | None = None,
    top_k: int | None = None,
    *,
    module: str | None = None,
    kind: str | None = None,
    occupation: str | None = None,
) -> list[ReferenceChunk]:
    """检索参考表达和知识片段。

    执行顺序：
    1. 先把 query 编成向量。
    2. 再把 module / kind / occupation 转成 Qdrant payload filter。
    3. 最后在线程池里调用同步 Qdrant client，避免阻塞事件循环。
    """
    vector = await embedding_service.embed(query)
    filters = {
        "module": module or section_code,
        "kind": kind,
        "occupation": occupation,
    }
    # 当前 vector_store_service 是同步实现，所以这里用 to_thread 包一层。
    results = await asyncio.to_thread(vector_store_service.search_similar, vector, top_k=top_k, filters=filters)
    return [
        ReferenceChunk(
            text=result.text,
            source=result.source,
            score=result.score,
            metadata={
                **result.metadata,
                "query": query,
                "sectionCode": section_code,
            },
        )
        for result in results
    ]
