"""测试当前配置的 LangChain ChatModel 集成。

用法：python scripts/check_model_connection.py
"""
from app.core.config import get_settings
from app.core.llm import agent_factory


def main() -> None:
    """使用当前本地模型配置发起一次简短对话。"""
    settings = get_settings()
    model = agent_factory.create_chat_model()
    response = model.invoke(
        f"用一句话回答：当前配置的 {settings.llm_provider} / {settings.llm_model_provider} 集成可以正常调用吗？"
    )
    print(response.content)


if __name__ == "__main__":
    main()
