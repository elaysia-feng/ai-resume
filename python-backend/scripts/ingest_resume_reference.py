"""导入简历参考 Markdown 到向量库。

用法：
    python scripts/ingest_resume_reference.py --dry-run
    python scripts/ingest_resume_reference.py
"""

from __future__ import annotations

import argparse
import os
import sys
from pathlib import Path
from typing import Any

from langchain_core.documents import Document


PROJECT_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_KNOWLEDGE_DIR = PROJECT_ROOT / "knowledge_base" / "resume_reference"

# 脚本位于 scripts 目录下，直接运行时 Python 默认找不到 src 包。
# 这里把 python-backend 加入 sys.path，让脚本可以复用项目里的 service。
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))


def parse_front_matter(text: str) -> tuple[dict[str, Any], str]:
    """解析 Markdown 顶部的简易 YAML front matter。"""
    lines = text.splitlines()
    if not lines or lines[0].strip() != "---":
        # 没有 front matter 时，仍允许导入正文，metadata 后面用默认值兜底。
        return {}, text.strip()

    end_index = None
    for index, line in enumerate(lines[1:], start=1):
        if line.strip() == "---":
            end_index = index
            break

    if end_index is None:
        # front matter 没有正常闭合时，不强行解析，避免误删正文。
        return {}, text.strip()

    metadata = parse_metadata_lines(lines[1:end_index])
    body = "\n".join(lines[end_index + 1 :]).strip()
    return metadata, body


def parse_metadata_lines(lines: list[str]) -> dict[str, Any]:
    """解析当前知识库使用的 key/value 和列表 metadata。"""
    metadata: dict[str, Any] = {}
    current_key: str | None = None

    for raw_line in lines:
        line = raw_line.rstrip()
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            continue

        if stripped.startswith("- ") and current_key:
            # 支持 tags 这类列表字段：
            # tags:
            #   - backend
            #   - ai
            value = clean_metadata_value(stripped[2:])
            current_value = metadata.setdefault(current_key, [])
            if isinstance(current_value, list):
                current_value.append(value)
            continue

        if ":" not in line:
            continue

        key, value = line.split(":", 1)
        current_key = key.strip()
        value = value.strip()
        # value 为空时先建列表，后续缩进的 "- xxx" 会继续追加。
        metadata[current_key] = clean_metadata_value(value) if value else []

    return metadata


def clean_metadata_value(value: str) -> str:
    """去掉简单引号，保持 metadata 可 JSON 序列化。"""
    value = value.strip()
    if len(value) >= 2 and value[0] == value[-1] and value[0] in {"'", '"'}:
        return value[1:-1]
    return value


def load_markdown_documents(knowledge_dir: Path) -> list[Document]:
    """读取目录下的 Markdown，并转成 LangChain Document。"""
    documents: list[Document] = []
    for path in sorted(knowledge_dir.glob("*.md")):
        # 明确使用 UTF-8，避免中文知识库在不同终端环境下乱码。
        text = path.read_text(encoding="utf-8")
        metadata, body = parse_front_matter(text)
        if not body:
            continue

        relative_source = path.relative_to(PROJECT_ROOT).as_posix()
        # source/module/kind 是当前向量检索和排查问题最常用的字段。
        # Markdown 里没写时给默认值，保证 payload 结构稳定。
        metadata.setdefault("source", relative_source)
        metadata.setdefault("module", "GENERAL")
        metadata.setdefault("kind", "resume_reference")
        # fileName 只用于 dry-run 和后台排查，不参与检索过滤。
        metadata["fileName"] = path.name

        documents.append(Document(page_content=body, metadata=metadata))

    return documents


def print_dry_run_summary(documents: list[Document]) -> None:
    """只输出短摘要，避免终端打印大量中文正文。"""
    print(f"documents={len(documents)}")
    for document in documents:
        metadata = document.metadata
        tags = metadata.get("tags") or []
        tag_text = ",".join(tags) if isinstance(tags, list) else str(tags)
        print(
            "file={file} module={module} kind={kind} chars={chars} tags={tags}".format(
                file=metadata.get("fileName"),
                module=metadata.get("module"),
                kind=metadata.get("kind"),
                chars=len(document.page_content),
                tags=tag_text,
            )
        )


def ingest_documents(documents: list[Document], deterministic_ids: bool) -> None:
    """调用项目现有入库服务写入 Qdrant。"""
    # 放在函数内部 import，保证 --dry-run 不会加载 embedding 模型或连接 Qdrant。
    from src.app.service.knowledge_ingest_service import knowledge_ingest_service

    knowledge_ingest_service.ingest_documents(documents, deterministic_ids=deterministic_ids)


def main() -> None:
    # 固定工作目录，确保 .env、相对路径和后端服务运行时一致。
    os.chdir(PROJECT_ROOT)

    parser = argparse.ArgumentParser(description="Ingest resume reference markdown files into Qdrant.")
    parser.add_argument(
        "--dir",
        type=Path,
        default=DEFAULT_KNOWLEDGE_DIR,
        help="Markdown knowledge directory.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Only parse files and print a short summary.",
    )
    parser.add_argument(
        "--random-ids",
        action="store_true",
        help="Use random Qdrant point ids. Default uses stable ids to overwrite repeated imports.",
    )
    args = parser.parse_args()

    knowledge_dir = args.dir.resolve()
    if not knowledge_dir.exists():
        raise FileNotFoundError(f"knowledge dir not found: {knowledge_dir}")

    documents = load_markdown_documents(knowledge_dir)
    if args.dry_run:
        # dry-run 只验证 Markdown 解析，不写向量库。
        print_dry_run_summary(documents)
        return

    if not documents:
        print("documents=0")
        return

    # 默认使用稳定 ID，重复导入会覆盖同一 source/chunk，避免免费向量库空间被重复数据吃掉。
    ingest_documents(documents, deterministic_ids=not args.random_ids)
    print(f"ingested_documents={len(documents)}")


if __name__ == "__main__":
    main()
