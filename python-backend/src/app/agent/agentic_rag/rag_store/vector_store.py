import os
from dotenv import load_dotenv
from elasticsearch import Elasticsearch

from pymilvus import MilvusClient, DataType
load_dotenv()


MILVUS_URI = os.getenv("MILVUS_URI", "http://localhost:19530")
COLLECTION_NAME = os.getenv("COLLECTION_NAME", "resume_reference_docs")
VECTOR_DIM = 1024  # text-embedding-v3 是 1024 维
ES_INDEX_NAME = os.getenv("ES_INDEX_NAME", "resume_reference_docs")
ELASTICSEARCH_URL = os.getenv("ELASTICSEARCH_URL", "http://localhost:9200")

milvus_client = MilvusClient(uri=MILVUS_URI)
es_client = Elasticsearch(
    ELASTICSEARCH_URL,
    request_timeout=30,
    max_retries=3,
    retry_on_timeout=True,
)



# =========================
# Milvus Collection
# =========================

def create_milvus_collection_if_not_exists(drop_old: bool = False):
    if not MILVUS_URI:
        raise ValueError("MILVUS_URI is required")
    if not COLLECTION_NAME:
        raise ValueError("COLLECTION_NAME is required")

    if milvus_client.has_collection(COLLECTION_NAME):
        if drop_old:
            milvus_client.drop_collection(COLLECTION_NAME)
        else:
            print(f"Milvus collection already exists: {COLLECTION_NAME}")
            return

    schema = MilvusClient.create_schema(
        auto_id=False,
        enable_dynamic_field=False,
    )

    # 主键：chunk 级别唯一 ID
    schema.add_field("id", DataType.VARCHAR, max_length=128, is_primary=True)

    # 原始文档定位
    schema.add_field("doc_id", DataType.VARCHAR, max_length=128)  # 例如 03-module-experience
    schema.add_field("source", DataType.VARCHAR, max_length=256)  # resume_reference/03-module-experience.md
    schema.add_field("file_name", DataType.VARCHAR, max_length=128)  # 03-module-experience.md
    schema.add_field("title", DataType.VARCHAR, max_length=256)  # 工作经历改写指南
    schema.add_field("heading_path", DataType.VARCHAR, max_length=512)  # 工作经历改写指南 > STAR 改写法

    # chunk 信息
    schema.add_field("chunk_index", DataType.INT64)
    schema.add_field("content_hash", DataType.VARCHAR, max_length=64)

    # 知识分类，来自 front matter 和文件名
    schema.add_field("module", DataType.VARCHAR, max_length=64)  # GENERAL / EXPERIENCE / PROJECTS
    schema.add_field("kind", DataType.VARCHAR, max_length=64)  # module_guide / jd_profile / interview_policy
    schema.add_field("doc_group", DataType.VARCHAR,
                     max_length=64)  # policy / module / career / example / jd / interview

    # RAG 检索过滤字段
    schema.add_field("scene_code", DataType.VARCHAR, max_length=64)  # resume_rewrite / job_match / interview / general
    schema.add_field("target_module", DataType.VARCHAR, max_length=64)  # EXPERIENCE / SKILLS / GENERAL
    schema.add_field("career_domain", DataType.VARCHAR, max_length=64)  # software_ai / product_ops / finance_hr
    schema.add_field("interview_type", DataType.VARCHAR, max_length=64)  # behavioral / technical / salary / scenario

    # tags 简化存储
    schema.add_field("primary_tag", DataType.VARCHAR, max_length=64)
    schema.add_field("tags_text", DataType.VARCHAR, max_length=512)

    # 状态和版本
    schema.add_field("version", DataType.VARCHAR, max_length=32)
    schema.add_field("status", DataType.VARCHAR, max_length=32)  # active / archived
    schema.add_field("updated_at", DataType.VARCHAR, max_length=32)

    # 文本和向量
    schema.add_field("text", DataType.VARCHAR, max_length=16384)

    schema.add_field(
        field_name="embedding",
        datatype=DataType.FLOAT_VECTOR,
        dim=VECTOR_DIM,
    )

    index_params = milvus_client.prepare_index_params()

    # 向量索引：语义相似度检索
    index_params.add_index(
        field_name="embedding",
        index_type="HNSW",
        metric_type="COSINE",
        params={
            "M": 16,
            "efConstruction": 200,
        },
    )

    # 标量索引：高频 metadata filter
    for field_name in [
        "doc_id",
        "module",
        "kind",
        "doc_group",
        "scene_code",
        "target_module",
        "career_domain",
        "interview_type",
        "primary_tag",
        "status",
        "version",
    ]:
        index_params.add_index(
            field_name=field_name,
            index_type="AUTOINDEX",
        )

    milvus_client.create_collection(
        collection_name=COLLECTION_NAME,
        schema=schema,
        index_params=index_params,
    )

    print(f"Milvus collection created: {COLLECTION_NAME}")


# =========================
# Elasticsearch Index
# =========================

def create_es_index_if_not_exists(drop_old: bool = False):
    if not es_client.ping():
        raise ConnectionError(f"Elasticsearch is not reachable: {ELASTICSEARCH_URL}")

    if es_client.indices.exists(index=ES_INDEX_NAME):
        if drop_old:
            es_client.indices.delete(index=ES_INDEX_NAME)
        else:
            print(f"ES index already exists: {ES_INDEX_NAME}")
            return

    es_client.indices.create(
        index=ES_INDEX_NAME,
        body={
            "mappings": {
                "properties": {
                    # 主键与文档定位
                    "id": {"type": "keyword"},
                    "doc_id": {"type": "keyword"},
                    "source": {"type": "keyword"},
                    "file_name": {"type": "keyword"},

                    # 可全文检索，也可精确过滤/聚合
                    "title": {
                        "type": "text",
                        "fields": {"keyword": {"type": "keyword"}}
                    },
                    "heading_path": {
                        "type": "text",
                        "fields": {"keyword": {"type": "keyword"}}
                    },

                    # chunk 信息
                    "chunk_index": {"type": "integer"},
                    "content_hash": {"type": "keyword"},

                    # 知识分类
                    "module": {"type": "keyword"},
                    "kind": {"type": "keyword"},
                    "doc_group": {"type": "keyword"},

                    # RAG 过滤字段
                    "scene_code": {"type": "keyword"},
                    "target_module": {"type": "keyword"},
                    "career_domain": {"type": "keyword"},
                    "interview_type": {"type": "keyword"},

                    # tags
                    "primary_tag": {"type": "keyword"},
                    "tags_text": {
                        "type": "text",
                        "fields": {"keyword": {"type": "keyword"}}
                    },

                    # 状态和版本
                    "version": {"type": "keyword"},
                    "status": {"type": "keyword"},
                    "updated_at": {"type": "keyword"},

                    # BM25 主检索字段
                    "text": {"type": "text"},
                }
            }
        },
    )

    print(f"ES index created: {ES_INDEX_NAME}")
