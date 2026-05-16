import uuid

from langchain_core.documents import Document
from langchain_text_splitters import RecursiveCharacterTextSplitter
from qdrant_client import QdrantClient
from qdrant_client.models import Distance, PayloadSchemaType, PointStruct, VectorParams

from ..config.settings import get_settings
from .embedding_service import embedding_service


class KnowledgeIngestService:
    """负责把参考样本切块并写入向量库。

    入库链路：
    1. 外部脚本把 Markdown 转成 Document。
    2. 本服务把 Document 切成 chunk，并保留 metadata。
    3. 每个 chunk 生成 embedding。
    4. 写入 Qdrant point：id + vector + payload。
    5. payload 里保留 content/source/module/kind/occupation/tags/chunkIndex。
    """

    def __init__(self) -> None:
        self.settings = get_settings()
        self.client: QdrantClient | None = None

    def _get_client(self) -> QdrantClient:
        if self.client is None:
            self.client = QdrantClient(
                url=self.settings.qdrant_url,
                api_key=self.settings.qdrant_api_key,
            )
        return self.client

    def _ensure_collection(self) -> None:
        """确保写入目标 collection 和 payload 索引存在。

        设计原因：
        1. 入库脚本可以在空库上直接运行。
        2. collection 向量维度必须匹配当前 embedding 模型。
        3. payload 索引用于后续 Agentic RAG 先过滤再向量检索。
        4. 入库和检索服务都建索引，是为了任意入口都能自愈。
        """
        client = self._get_client()
        if not client.collection_exists(collection_name=self.settings.qdrant_collection_name):
            client.create_collection(
                collection_name=self.settings.qdrant_collection_name,
                vectors_config=VectorParams(
                    size=self.settings.embedding_dimension,
                    distance=Distance.COSINE,
                ),
            )

        # 检索时会先按这些 payload 字段粗过滤，再做向量相似度搜索。
        for field_name in ["module", "kind", "occupation", "source"]:
            client.create_payload_index(
                collection_name=self.settings.qdrant_collection_name,
                field_name=field_name,
                field_schema=PayloadSchemaType.KEYWORD,
            )

    def ingest_documents(self, documents: list[Document], deterministic_ids: bool = False) -> None:
        """执行知识入库流程。

        执行顺序：
        1. split_document：把长文档切成适合召回的小片段。
        2. _ensure_collection：确保 Qdrant 写入目标存在。
        3. embed_texts：批量生成向量。
        4. build_point_id：生成随机或稳定 point id。
        5. upsert：写入 Qdrant；稳定 id 会覆盖同来源同 chunk。
        """
        chunked_docs: list[Document] = []
        for document in documents:
            # 每个 Markdown 先切成适合召回的小 chunk，同时保留原始 metadata。
            chunked_docs.extend(self.split_document(document))

        if not chunked_docs:
            return

        self._ensure_collection()
        # 这里会加载本地 embedding 模型；脚本 dry-run 不会走到这里。
        vectors = embedding_service.embed_texts([doc.page_content for doc in chunked_docs])
        # 入库时把检索需要的标准字段和原始 metadata 一起保留。
        points = [
            PointStruct(
                # 默认仍使用随机 ID，避免影响其他调用方；脚本导入时会传 deterministic_ids=True。
                id=self.build_point_id(doc, deterministic_ids),
                vector=vector,
                payload={
                    "content": doc.page_content,
                    "source": doc.metadata.get("source"),
                    "module": doc.metadata.get("module"),
                    **doc.metadata,
                },
            )
            for doc, vector in zip(chunked_docs, vectors, strict=True)
        ]
        self._get_client().upsert(
            collection_name=self.settings.qdrant_collection_name,
            points=points,
        )

    def build_point_id(self, document: Document, deterministic_ids: bool) -> str:
        """构造 Qdrant point id，脚本重复导入时可覆盖同一来源 chunk。

        ID 策略：
        1. deterministic_ids=False：使用随机 UUID，适合临时或追加数据。
        2. deterministic_ids=True：使用 collection + source + chunkIndex 生成 UUID5。
        3. 同一文件同一 chunk 重复导入时会覆盖，不会重复占空间。
        4. source/chunkIndex 缺失时退回随机 UUID，避免误覆盖不同内容。
        """
        if not deterministic_ids:
            return str(uuid.uuid4())

        source = document.metadata.get("source")
        chunk_index = document.metadata.get("chunkIndex")
        if source is None or chunk_index is None:
            # 缺少稳定来源时退回随机 ID，避免不同文档误覆盖。
            return str(uuid.uuid4())
        # Qdrant upsert 遇到相同 point id 会覆盖旧数据。
        # 用 collection + source + chunkIndex 生成 UUID，能让同一知识库重复导入保持幂等。
        point_key = f"{self.settings.qdrant_collection_name}:{source}:{chunk_index}"
        return str(uuid.uuid5(uuid.NAMESPACE_URL, point_key))

    def split_text(
        self,
        text: str,
        chunk_size: int = 500,
        chunk_overlap: int = 50,
    ) -> list[str]:
        """按业务规则切分参考文本。

        切分策略：
        1. chunk_size=500，保证单个片段足够短，适合向量召回。
        2. chunk_overlap=50，避免标题、上下文和正文被完全切断。
        3. 当前知识库是中文 Markdown，RecursiveCharacterTextSplitter 足够使用。
        """
        splitter = RecursiveCharacterTextSplitter(
            chunk_size=chunk_size,
            chunk_overlap=chunk_overlap,
        )
        return splitter.split_text(text)

    def split_document(self, document: Document) -> list[Document]:
        """将 Document 切分成多个 Document，保持 metadata 不变。

        输出规则：
        1. 每个 chunk 都继承原始 metadata。
        2. 额外写入 chunkIndex，便于排查和生成稳定 point id。
        3. page_content 只放当前 chunk 正文，不重复塞完整文档。
        """
        chunks = self.split_text(document.page_content)
        return [
            # chunkIndex 用于生成稳定 point id，也方便后续定位召回片段来自文件的哪一块。
            Document(page_content=chunk, metadata={**document.metadata, "chunkIndex": index})
            for index, chunk in enumerate(chunks)
        ]


knowledge_ingest_service = KnowledgeIngestService()
