from langchain_core.documents import Document
from langchain_text_splitters import MarkdownHeaderTextSplitter, RecursiveCharacterTextSplitter


def strip_front_matter(markdown_content: str) -> str:
    lines = markdown_content.splitlines()
    if not lines or lines[0].strip() != "---":
        return markdown_content

    for index in range(1, len(lines)):
        if lines[index].strip() == "---":
            return "\n".join(lines[index + 1:]).strip()

    return markdown_content


async def md_splitter(path: str, chunk_size: int = 500, chunk_overlap: int = 100) -> list[Document]:
    print(f"正在读取 {path} ...")

    with open(path, "r", encoding="utf-8") as f:
        markdown_content = f.read()
    markdown_content = strip_front_matter(markdown_content)

    headers_to_split_on = [
        ("#", "h1"),
        ("##", "h2"),
        ("###", "h3"),
    ]

    # 第一步：按标题分割
    header_splitter = MarkdownHeaderTextSplitter(headers_to_split_on=headers_to_split_on, strip_headers=False)
    splits = header_splitter.split_text(markdown_content)

    # 第二步：对每个 split 再做固定长度切分
    text_splitter = RecursiveCharacterTextSplitter(chunk_size=chunk_size, chunk_overlap=chunk_overlap)
    docs = text_splitter.split_documents(splits)

    return docs
