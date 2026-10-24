"""测试当前生效的 LLM 通道（mimo / deepseek 由 .env 的 LLM_PROVIDER 决定）。

用法：python scripts/test_mimo_chat.py
"""
from dotenv import load_dotenv
from langchain_openai import ChatOpenAI

from src.app.config.settings import get_settings


def main() -> None:
    load_dotenv()

    settings = get_settings()
    api_key = settings.effective_llm_api_key
    model = settings.effective_llm_chat_model
    base_url = settings.effective_llm_base_url

    if not api_key or api_key.startswith("your_"):
        raise RuntimeError(f"请先在 .env 里配置 {settings.llm_provider.upper()} 的 API key")

    llm = ChatOpenAI(
        model=model,
        api_key=api_key,
        base_url=base_url,
        default_headers={"api-key": api_key} if settings.llm_provider.lower() == "mimo" else None,
        temperature=0.2,
        timeout=settings.llm_timeout_seconds,
    )

    response = llm.invoke(f"用一句话回答：你现在能正常调用 {settings.llm_provider} 吗？")
    print(response.content)


if __name__ == "__main__":
    main()
