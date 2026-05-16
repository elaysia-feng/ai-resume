from typing import Any

from qdrant_client import QdrantClient
from qdrant_client.models import Distance, FieldCondition, Filter, MatchValue, PayloadSchemaType, VectorParams

from ..config.settings import get_settings
from ..dto.vector_dto import VectorSearchResult


class VectorStoreService:
    """负责向量检索。

    查询链路：
    1. 确保 Qdrant collection 和 payload 索引存在。
    2. 把业务过滤条件转成 Qdrant Filter。
    3. 在过滤后的候选集合中执行向量相似度搜索。
    4. 把 Qdrant point 转成项目内部的 VectorSearchResult。
    """

    def __init__(self) -> None:
        self.settings = get_settings()
        self.client: QdrantClient | None = None
        self.collection_name = self.settings.qdrant_collection_name

    def _get_client(self) -> QdrantClient:
        if self.client is None:
            self.client = QdrantClient(
                url=self.settings.qdrant_url,
                api_key=self.settings.qdrant_api_key,
            )
        return self.client

    def _ensure_collection(self) -> None:
        """确保 collection 和检索用 payload 索引存在。

        这里是读路径兜底：
        1. 如果用户先检索、还没入库，也能自动创建 collection。
        2. 向量维度必须和 embedding 模型一致。
        3. payload 索引用于 module/kind/occupation/source 过滤。
        4. create_payload_index 多次调用由 Qdrant 处理，保持幂等。
        """
        client = self._get_client()
        if not client.collection_exists(collection_name=self.collection_name):
            # 检索前兜底建集合，确保维度和本地 BGE 配置保持一致。
            client.create_collection(
                collection_name=self.collection_name,
                vectors_config=VectorParams(
                    size=self.settings.embedding_dimension,
                    distance=Distance.COSINE,
                ),
            )

        # Agentic RAG 会按模块、知识类型和职业画像过滤，先建 keyword 索引提升过滤效率。
        for field_name in ["module", "kind", "occupation", "source"]:
            client.create_payload_index(
                collection_name=self.collection_name,
                field_name=field_name,
                field_schema=PayloadSchemaType.KEYWORD,
            )

    def search_similar(
        self,
        query_vector: list[float],
        top_k: int | None = None,
        filter_module: str | None = None,
        filters: dict[str, Any] | None = None,
    ) -> list[VectorSearchResult]:
        """按向量和 payload filter 检索相似片段。

        参数含义：
        1. query_vector：用户 query 经过 embedding 后的向量。
        2. top_k：最多返回多少条，未传时使用配置默认值。
        3. filter_module：兼容旧调用，只按 module 过滤。
        4. filters：新调用使用的多字段过滤条件。

        返回结果：
        1. content 映射为 text。
        2. source 单独返回，便于排查来源文件。
        3. 其他 payload 放入 metadata，供 Retriever/Rewriter 调试。
        """
        self._ensure_collection()
        search_filter = self.build_filter(filter_module, filters)

        client = self._get_client()
        limit = top_k or self.settings.qdrant_top_k

        # 兼容不同版本的 qdrant-client：新版使用 query_points，旧版使用 search。
        if hasattr(client, "query_points"):
            response = client.query_points(
                collection_name=self.collection_name,
                query=query_vector,
                limit=limit,
                query_filter=search_filter,
                score_threshold=self.settings.qdrant_score_threshold,
                with_payload=True,
                with_vectors=False,
            )
            results = response.points
        else:
            results = client.search(
                collection_name=self.collection_name,
                query_vector=query_vector,
                limit=limit,
                query_filter=search_filter,
                score_threshold=self.settings.qdrant_score_threshold,
            )

        search_results: list[VectorSearchResult] = []
        for result in results:
            payload = result.payload or {}
            search_results.append(
                VectorSearchResult(
                    text=payload.get("content", ""),
                    score=result.score,
                    source=payload.get("source"),
                    metadata={key: value for key, value in payload.items() if key != "content"},
                )
            )
        return search_results

    def build_filter(self, filter_module: str | None = None, filters: dict[str, Any] | None = None) -> Filter | None:
        """把业务过滤条件转成 Qdrant payload filter。

        转换规则：
        1. filters 是主入口，可包含 module/kind/occupation/source。
        2. filter_module 是旧参数，只在 filters 没有 module 时补进去。
        3. 空值不参与过滤，避免把 None 传给 Qdrant。
        4. 没有任何条件时返回 None，表示全库向量检索。
        """
        merged_filters = dict(filters or {})
        if filter_module and not merged_filters.get("module"):
            merged_filters["module"] = filter_module

        conditions = [
            FieldCondition(key=key, match=MatchValue(value=value))
            for key, value in merged_filters.items()
            if value
        ]
        return Filter(must=conditions) if conditions else None


vector_store_service = VectorStoreService()
