import asyncio
import hashlib
import os
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Tuple

from dashscope import TextEmbedding
from dotenv import load_dotenv

try:
    from .md_splitter import md_splitter
    from .vector_store import (
        COLLECTION_NAME,
        ES_INDEX_NAME,
        create_es_index_if_not_exists,
        create_milvus_collection_if_not_exists,
        es_client,
        milvus_client,
    )
except ImportError:
    from md_splitter import md_splitter
    from vector_store import (
        COLLECTION_NAME,
        ES_INDEX_NAME,
        create_es_index_if_not_exists,
        create_milvus_collection_if_not_exists,
        es_client,
        milvus_client,
    )

load_dotenv()

PYTHON_BACKEND_DIR = Path(__file__).resolve().parents[5]
DEFAULT_KB_DIR = PYTHON_BACKEND_DIR / "knowledge_base" / "resume_reference"
DEFAULT_VERSION = os.getenv("RAG_KB_VERSION", "v1")


def parse_front_matter(content: str) -> Tuple[Dict[str, Any], str]:
    """解析 Markdown 顶部的 YAML front matter，只处理当前知识库需要的简单格式。"""
    lines = content.splitlines()
    if not lines or lines[0].strip() != "---":
        return {}, content

    end_index = None
    for index in range(1, len(lines)):
        if lines[index].strip() == "---":
            end_index = index
            break

    if end_index is None:
        return {}, content

    metadata: Dict[str, Any] = {}
    current_list_key = None
    for line in lines[1:end_index]:
        stripped = line.strip()
        if not stripped:
            continue
        if stripped.startswith("- ") and current_list_key:
            metadata.setdefault(current_list_key, []).append(stripped[2:].strip())
            continue
        if ":" not in stripped:
            continue

        key, value = stripped.split(":", 1)
        key = key.strip()
        value = value.strip()
        if value:
            metadata[key] = value
            current_list_key = None
        else:
            metadata[key] = []
            current_list_key = key

    markdown_body = "\n".join(lines[end_index + 1 :]).strip()
    return metadata, markdown_body


def infer_doc_group(file_name: str, kind: str) -> str:
    if kind == "rewrite_policy":
        return "policy"
    if kind in {"section_patterns", "module_guide"}:
        return "module"
    if kind in {"keyword_map", "jd_profile"}:
        return "jd"
    if kind == "occupation_profile":
        return "career"
    if kind == "example_good":
        return "example"
    if kind.startswith("interview_"):
        return "interview"
    if file_name == "README.md":
        return "readme"
    return "general"


def infer_scene_code(doc_group: str) -> str:
    if doc_group == "jd":
        return "job_match"
    if doc_group == "interview":
        return "interview"
    if doc_group in {"policy", "module", "career", "example"}:
        return "resume_rewrite"
    return "general"


def infer_career_domain(file_name: str) -> str:
    mapping = {
        "10-career-software-ai": "software_ai",
        "11-career-hardware-embedded": "hardware_embedded",
        "12-career-education-training": "education_training",
        "13-career-product-operations": "product_ops",
        "14-career-sales-marketing": "sales_marketing",
        "15-career-design-content": "design_content",
        "16-career-finance-admin-hr": "finance_admin_hr",
        "17-career-service-healthcare": "service_healthcare",
        "18-career-manufacturing-supply": "manufacturing_supply",
        "20-examples-software-engineer": "software_ai",
        "21-examples-product-ops": "product_ops",
        "30-jd-tech-startup": "software_ai",
        "31-jd-finance-corporate": "finance_admin_hr",
        "32-jd-education-government": "education_training",
        "42-interview-technical-dev": "software_ai",
        "43-interview-technical-product": "product_ops",
        "46-interview-scenario-sales": "sales_marketing",
    }
    stem = Path(file_name).stem
    return mapping.get(stem, "GENERAL")


def infer_interview_type(file_name: str, doc_group: str) -> str:
    if doc_group != "interview":
        return "GENERAL"

    mapping = {
        "40-interview-policy": "policy",
        "41-interview-behavioral": "behavioral",
        "42-interview-technical-dev": "technical",
        "43-interview-technical-product": "technical",
        "44-interview-industry-common": "industry",
        "45-interview-salary-negotiation": "salary",
        "46-interview-scenario-sales": "scenario",
        "47-interview-fresh-graduate": "fresh",
    }
    return mapping.get(Path(file_name).stem, "general")


def build_heading_path(metadata: Dict[str, Any], fallback_title: str) -> str:
    headings = [
        metadata.get("h1"),
        metadata.get("h2"),
        metadata.get("h3"),
    ]
    heading_path = " > ".join(heading for heading in headings if heading)
    return heading_path or fallback_title


def build_rows(md_path: Path, updated_at: str) -> List[Dict[str, Any]]:
    content = md_path.read_text(encoding="utf-8")
    metadata, markdown_body = parse_front_matter(content)

    source = metadata.get("source", f"resume_reference/{md_path.name}")
    module = metadata.get("module", "GENERAL")
    kind = metadata.get("kind", "general")
    tags = metadata.get("tags", [])
    doc_group = infer_doc_group(md_path.name, kind)
    title = next((line.lstrip("#").strip() for line in markdown_body.splitlines() if line.startswith("# ")), md_path.stem)
    docs = asyncio.run(md_splitter(str(md_path)))

    rows = []
    for chunk_index, doc in enumerate(docs):
        text = doc.page_content
        content_hash = hashlib.sha256(text.encode("utf-8")).hexdigest()
        chunk_id = hashlib.sha256(f"{source}:{chunk_index}:{content_hash}".encode("utf-8")).hexdigest()[:32]

        rows.append(
            {
                "id": chunk_id,
                "doc_id": md_path.stem,
                "source": source,
                "file_name": md_path.name,
                "title": title,
                "heading_path": build_heading_path(doc.metadata, title),
                "chunk_index": chunk_index,
                "content_hash": content_hash,
                "module": module,
                "kind": kind,
                "doc_group": doc_group,
                "scene_code": infer_scene_code(doc_group),
                "target_module": module,
                "career_domain": infer_career_domain(md_path.name),
                "interview_type": infer_interview_type(md_path.name, doc_group),
                "primary_tag": tags[0] if tags else "GENERAL",
                "tags_text": ",".join(tags),
                "version": DEFAULT_VERSION,
                "status": "active",
                "updated_at": updated_at,
                "text": text,
            }
        )

    return rows


def embed_rows(rows: List[Dict[str, Any]]) -> None:
    """逐条生成 embedding，避免不同 SDK 版本对批量输入返回结构不一致。"""
    for index, row in enumerate(rows, start=1):
        result = TextEmbedding.call(model="text-embedding-v3", input=row["text"])
        row["embedding"] = result["output"]["embeddings"][0]["embedding"]
        print(f"embedded {index}/{len(rows)}: {row['source']}#{row['chunk_index']}")


def write_to_es(rows: List[Dict[str, Any]]) -> None:
    for row in rows:
        es_body = {key: value for key, value in row.items() if key != "embedding"}
        es_client.index(index=ES_INDEX_NAME, id=row["id"], document=es_body)
    es_client.indices.refresh(index=ES_INDEX_NAME)


def write_to_milvus(rows: List[Dict[str, Any]]) -> None:
    milvus_client.upsert(collection_name=COLLECTION_NAME, data=rows)
    milvus_client.flush(collection_name=COLLECTION_NAME)
    milvus_client.load_collection(collection_name=COLLECTION_NAME)


def import_resume_reference(kb_dir: Path = DEFAULT_KB_DIR, drop_old: bool = False) -> None:
    if not ES_INDEX_NAME:
        raise ValueError("ES_INDEX_NAME is required")
    if not COLLECTION_NAME:
        raise ValueError("COLLECTION_NAME is required")
    if not kb_dir.exists():
        raise FileNotFoundError(f"knowledge dir not found: {kb_dir}")

    create_es_index_if_not_exists(drop_old=drop_old)
    create_milvus_collection_if_not_exists(drop_old=drop_old)

    updated_at = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    rows: List[Dict[str, Any]] = []
    for md_path in sorted(kb_dir.glob("*.md")):
        rows.extend(build_rows(md_path, updated_at))

    embed_rows(rows)
    write_to_es(rows)
    write_to_milvus(rows)
    print(f"imported files={len(list(kb_dir.glob('*.md')))} chunks={len(rows)}")


if __name__ == "__main__":
    drop_old = os.getenv("DROP_OLD_RAG_INDEX", "false").lower() == "true"
    import_resume_reference(drop_old=drop_old)
