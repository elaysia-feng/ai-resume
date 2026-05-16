import asyncio
from pathlib import Path

from sentence_transformers import SentenceTransformer

from ..config.settings import get_settings


class EmbeddingService:
    """统一封装本地 BGE 向量模型。"""

    def __init__(self) -> None:
        self.settings = get_settings()
        self.model: SentenceTransformer | None = None

    def _resolve_cache_folder(self) -> str:
        configured_folder = Path(self.settings.embedding_cache_folder)
        try:
            configured_folder.mkdir(parents=True, exist_ok=True)
            return str(configured_folder)
        except OSError:
            # 配置路径不可写时，回退到项目内缓存，避免服务启动直接失败。
            fallback_folder = Path(__file__).resolve().parents[3] / ".cache" / "embedding_models"
            fallback_folder.mkdir(parents=True, exist_ok=True)
            return str(fallback_folder)

    def _get_model(self) -> SentenceTransformer:
        if self.model is None:
            # 按需加载本地模型，避免 import 阶段就触发下载或权限问题。
            self.model = SentenceTransformer(
                self.settings.embedding_model_name,
                cache_folder=self._resolve_cache_folder(),
                device=self.settings.embedding_device,
            )
        return self.model

    async def embed(self, text: str) -> list[float]:
        """生成单条文本向量。

        SentenceTransformer.encode 是同步 CPU/IO 操作。
        放到线程池里执行，可以避免阻塞 FastAPI / worker 的 asyncio 事件循环。
        """
        return await asyncio.to_thread(self.embed_sync, text)

    def embed_sync(self, text: str) -> list[float]:
        """同步生成单条文本向量，供线程池调用。"""
        vector = self._get_model().encode(
            text,
            normalize_embeddings=self.settings.embedding_normalize,
        )
        return vector.tolist()

    def embed_texts(self, texts: list[str]) -> list[list[float]]:
        """批量生成文本向量。"""
        if not texts:
            return []

        vectors = self._get_model().encode(
            texts,
            normalize_embeddings=self.settings.embedding_normalize,
        )
        return vectors.tolist()


embedding_service = EmbeddingService()
