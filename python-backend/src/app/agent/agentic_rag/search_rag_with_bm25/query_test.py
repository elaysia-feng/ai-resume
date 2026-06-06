import sys
from pathlib import Path
from pprint import pprint

sys.path.append(str(Path(__file__).resolve().parents[4]))

from app.agent.agentic_rag.rag_store.vector_store import COLLECTION_NAME, ES_INDEX_NAME, es_client, milvus_client
from app.agent.agentic_rag.search_rag_with_bm25.query import embed_text


QUERY_TEXT = "Java 后端项目经历怎么写"


if __name__ == "__main__":
    es_response = es_client.search(
        index=ES_INDEX_NAME,
        size=3,
        query={
            "bool": {
                "must": {
                    "multi_match": {
                        "query": QUERY_TEXT,
                        "fields": [
                            "text^3",
                            "title^2",
                            "heading_path^1.5",
                            "tags_text",
                        ],
                    }
                },
                "filter": [
                    {"term": {"status": "active"}},
                    {"term": {"scene_code": "resume_rewrite"}},
                ],
            }
        },
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

    print("\n===== ES raw response =====")
    pprint(es_response.body, width=140, sort_dicts=False)

    milvus_response = milvus_client.search(
        collection_name=COLLECTION_NAME,
        data=[embed_text(QUERY_TEXT)],
        anns_field="embedding",
        limit=3,
        filter='status == "active" and scene_code == "resume_rewrite"',
        search_params={
            "metric_type": "COSINE",
            "params": {"ef": 64},
        },
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

    print("\n===== Milvus raw response =====")
    pprint(milvus_response, width=140, sort_dicts=False)
