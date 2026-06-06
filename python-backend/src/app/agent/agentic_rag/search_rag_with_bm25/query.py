import os
from typing import Any, Dict, List, Optional

from dotenv import load_dotenv
from langchain_core.tools import tool

load_dotenv()

ES_INDEX_NAME = os.getenv("ES_INDEX_NAME", "resume_reference_docs")
MILVUS_URI = os.getenv("MILVUS_URI", "http://localhost:19530")
COLLECTION_NAME = os.getenv("COLLECTION_NAME", "resume_reference_docs")
ELASTICSEARCH_URL = os.getenv("ELASTICSEARCH_URL", "http://localhost:9200")
es_client = None
milvus_client = None


# 向量化

def get_es_client():
    global es_client
    if es_client is None:
        from elasticsearch import Elasticsearch

        es_client = Elasticsearch(
            ELASTICSEARCH_URL,
            request_timeout=30,
            max_retries=3,
            retry_on_timeout=True,
        )
    return es_client


def get_milvus_client():
    global milvus_client
    if not MILVUS_URI:
        raise ValueError("MILVUS_URI is required")
    if milvus_client is None:
        from pymilvus import MilvusClient

        milvus_client = MilvusClient(uri=MILVUS_URI)
    return milvus_client


def embed_text(text: str) -> List[float]:
    from dashscope import TextEmbedding

    result = TextEmbedding.call(
        model="text-embedding-v3",
        input=text,
    )

    return result["output"]["embeddings"][0]["embedding"]


def query_with_bm25(
    text: str,
    top_k: int = 5,
    filters: Optional[Dict[str, Any]] = None,
) -> List[Dict[str, Any]]:
    if not ES_INDEX_NAME:
        raise ValueError("ES_INDEX_NAME is required")
    if not text.strip():
        return []

    client = get_es_client()

    # 默认只查有效知识片段，避免召回已归档或失效内容。
    filter_clauses = [{"term": {"status": "active"}}]

    # 额外过滤条件用于限定知识范围，例如 scene_code/doc_group/module。
    if filters:
        for field_name, field_value in filters.items():
            if field_value is None:
                continue
            filter_clauses.append({"term": {field_name: field_value}})

    response = client.search(
        index=ES_INDEX_NAME,
        size=top_k,
        query={
            "bool": {
                # multi_match 会对多个 """text""" 字段做 BM25 全文检索。
                # ^ 后面的数字是权重，正文 text 最重要，标题和标题路径辅助召回。
                "must": {
                    "multi_match": {
                        "query": text,
                        "fields": [
                            "text^3",
                            "title^2",
                            "heading_path^1.5",
                            "tags_text",
                        ],
                    }
                },
                # filter 不参与相关性打分，只负责缩小候选范围。
                "filter": filter_clauses,
            }
        },
        # 只返回 RAG 需要拼上下文和溯源的字段。
        source=[
            "id",
            "doc_id",
            "source",
            "title",
            "heading_path",
            "text",
            "module",
            "kind",
            "doc_group",
            "scene_code",
            "target_module",
            "career_domain",
            "interview_type",
            "primary_tag",
            "tags_text",
            "status",
        ],
    )

    results = []
    for hit in response["hits"]["hits"]:
        item = hit["_source"]
        # score 是 ES/BM25 的相关性分数，后续可用于和向量召回结果融合排序。
        item["score"] = hit["_score"]
        results.append(item)
    return results


def query_with_milvus(
    text: str,
    top_k: int = 5,
    filters: Optional[Dict[str, Any]] = None,
) -> List[Dict[str, Any]]:
    if not COLLECTION_NAME:
        raise ValueError("COLLECTION_NAME is required")
    if not text.strip():
        return []

    client = get_milvus_client()
    query_embedding = embed_text(text)

    # 默认只查有效知识片段，和 BM25 查询保持一致。
    filter_parts = ['status == "active"']

    # Milvus 的 filter 是字符串表达式，例如 scene_code == "interview"。
    if filters:
        for field_name, field_value in filters.items():
            if field_value is None:
                continue
            if isinstance(field_value, bool):
                filter_parts.append(f"{field_name} == {str(field_value).lower()}")
            elif isinstance(field_value, (int, float)):
                filter_parts.append(f"{field_name} == {field_value}")
            else:
                safe_value = str(field_value).replace("\\", "\\\\").replace('"', '\\"')
                filter_parts.append(f'{field_name} == "{safe_value}"')

    response = client.search(
        collection_name=COLLECTION_NAME,
        data=[query_embedding],
        anns_field="embedding",
        limit=top_k,
        filter=" and ".join(filter_parts),
        search_params={
            "metric_type": "COSINE",
            "params": {
                # ef 是 HNSW 查询宽度，越大召回越稳，但查询越慢。
                "ef": 64,
            },
        },
        # 只返回 RAG 需要拼上下文和溯源的字段。
        output_fields=[
            "id",
            "doc_id",
            "source",
            "title",
            "heading_path",
            "text",
            "module",
            "kind",
            "doc_group",
            "scene_code",
            "target_module",
            "career_domain",
            "interview_type",
            "primary_tag",
            "tags_text",
            "status",
        ],
    )

    results = []
    for hit in response[0]:
        item = dict(hit["entity"])
        # distance 是 Milvus 返回的向量相似度分数，后续可用于和 BM25 分数融合。
        item["score"] = hit["distance"]
        results.append(item)
    return results


@tool("rag_query")
def rag_query(
    text: str,
    top_k: int = 5,
    filters: Optional[Dict[str, Any]] = None,
) -> List[Dict[str, Any]]:
    """
    检索简历/求职/面试知识库，返回可作为 RAG 上下文的参考片段。

    适合调用的情况：
    - 用户要求优化简历、改写工作经历、项目经历、技能、个人总结、教育经历、实习经历。
    - 用户要求根据 JD 做关键词匹配、岗位匹配、差距分析、简历针对性修改。
    - 用户要求生成或回答面试题，包括行为面试、技术面试、产品运营面试、谈薪、情景模拟。
    - 需要查询简历写作规则、事实边界、岗位画像、JD 模式、优秀表达范例。

    参数：
    - text: 用户问题或需要检索的查询文本，应该包含当前任务的核心意图。
    - top_k: 返回片段数量，默认 5。
    - filters: 可选过滤条件，用于限定检索范围。

    常用 filters 示例：
    - {"scene_code": "resume_rewrite"}：查简历改写知识。
    - {"scene_code": "job_match"}：查 JD 匹配知识。
    - {"scene_code": "interview"}：查面试知识。
    - {"doc_group": "module", "module": "PROJECTS"}：查项目经历写法。
    - {"doc_group": "career", "career_domain": "software_ai"}：查软件/AI 岗位画像。
    - {"scene_code": "interview", "interview_type": "technical"}：查技术面试知识。

    返回：
    - text: 知识片段正文。
    - title/heading_path/source: 片段来源，便于引用和组织回答。
    - bm25_score/vector_score/retrieval_sources: 混合检索分数和召回来源。

    检索方式：
    - BM25 用于关键词、标题、术语命中。
    - Milvus 向量检索用于语义相似召回。
    - 同一个 chunk 同时被两路命中时会优先返回。
    """
    es_result = []
    milvus_result = []

    retrieval_errors = []

    try:
        es_result = query_with_bm25(text, top_k=top_k, filters=filters)
    except Exception as exc:
        retrieval_errors.append(f"bm25: {exc}")

    try:
        milvus_result = query_with_milvus(text, top_k=top_k, filters=filters)
    except Exception as exc:
        retrieval_errors.append(f"milvus: {exc}")

    if not es_result and not milvus_result and retrieval_errors:
        raise RuntimeError("混合检索失败: " + " | ".join(retrieval_errors))

    merged = {}

    # 先放 BM25 结果，适合命中明确关键词、标题、术语。
    for index, item in enumerate(es_result):
        chunk_id = item["id"]
        merged[chunk_id] = {
            **item,
            "bm25_score": item.get("score", 0),
            "vector_score": 0,
            "retrieval_sources": ["bm25"],
            # rank 用来保留原始排序信息，越靠前越重要。
            "bm25_rank": index + 1,
            "vector_rank": None,
        }

    # 再合并 Milvus 结果；同一个 chunk 同时命中两路检索时优先级更高。
    for index, item in enumerate(milvus_result):
        chunk_id = item["id"]
        if chunk_id in merged:
            merged_item = merged[chunk_id]
            merged_item["vector_score"] = item.get("score", 0)
            merged_item["vector_rank"] = index + 1
            merged_item["retrieval_sources"].append("milvus")
        else:
            merged[chunk_id] = {
                **item,
                "bm25_score": 0,
                "vector_score": item.get("score", 0),
                "retrieval_sources": ["milvus"],
                "bm25_rank": None,
                "vector_rank": index + 1,
            }

    results = list(merged.values())

    # 简单融合排序：两路都命中的排前面；单路命中时保留各自分数和排名信号。
    def sort_key(item: Dict[str, Any]):
        source_count = len(item["retrieval_sources"])
        bm25_rank_score = 1 / item["bm25_rank"] if item["bm25_rank"] else 0
        vector_rank_score = 1 / item["vector_rank"] if item["vector_rank"] else 0
        return source_count, bm25_rank_score + vector_rank_score, item["bm25_score"], item["vector_score"]

    results.sort(key=sort_key, reverse=True)

    return results[:top_k]
