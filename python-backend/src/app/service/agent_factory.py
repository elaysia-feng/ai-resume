import asyncio
from typing import TypeVar

from langchain.agents import create_agent
from langchain_core.messages import BaseMessage
from langchain_openai import ChatOpenAI
from pydantic import BaseModel

from src.app.config.settings import get_settings

StructuredModel = TypeVar("StructuredModel", bound=BaseModel)


class AgentFactory:
    """负责创建和调用 LangChain Agent。

    使用方式：
    1. 各 LangGraph 节点准备 messages 和 response_model。
    2. invoke_agent 创建结构化 Agent。
    3. LangChain 调用底层 ChatModel。
    4. 返回 Pydantic 结构化结果，节点再写入 state。
    """

    def __init__(self) -> None:
        settings = get_settings()
        # 1. 这里限制的是单个 Python 进程内同时打到 LLM 的请求数。
        # 2. 多个 Python worker 进程部署时，总并发约等于进程数 * 这个值。
        # 3. 这个限制保护模型 API 和本机连接资源，不等于 RabbitMQ 的 run 队列容量。
        self._llm_semaphore = asyncio.Semaphore(max(1, settings.agent_llm_max_concurrent_calls))

    def create_chat_model(self) -> ChatOpenAI:
        """创建底层 ChatModel，供 create_agent 使用。

        配置来源：
        1. LLM API key / base_url / model 从 settings 读取。
        2. temperature 固定较低，减少结构化输出波动。
        3. 未配置 key 时抛错，让节点走本地兜底逻辑。
        """
        settings = get_settings()
        api_key = settings.effective_llm_api_key
        if not api_key or api_key.startswith("your_"):
            raise ValueError("LLM API key 未配置，跳过真实 Agent 调用")
        return ChatOpenAI(
            model=settings.effective_llm_chat_model,
            temperature=0.2,
            api_key=api_key,
            base_url=settings.effective_llm_base_url,
            timeout=settings.llm_timeout_seconds,
            default_headers=build_provider_headers(settings.llm_provider, api_key),
        )

    def create_structured_agent(
        self,
        response_model: type[StructuredModel],
        *,
        system_prompt: str | None = None,
    ):
        """创建带结构化输出约束的 LangChain Agent。

        这里用的是 LangChain 1.x 的 create_agent。
        每个 LangGraph 节点可以把它当成自己的专用 Agent 来调用。

        当前设计：
        1. tools=[]，节点内部自己编排工具调用。
        2. response_format=response_model，要求模型直接返回结构化结果。
        3. system_prompt 可选，多数节点已经把 SystemMessage 放在 messages 里。
        """
        return create_agent(
            model=self.create_chat_model(),
            tools=[],
            system_prompt=system_prompt,
            response_format=response_model,
        )

    async def invoke_agent(
        self,
        messages: list[BaseMessage],
        response_model: type[StructuredModel],
        *,
        system_prompt: str | None = None,
    ) -> StructuredModel:
        """通过 create_agent 调用模型并获取结构化结果。

        解析顺序：
        1. 优先读取 LangChain 的 structured_response。
        2. 如果 structured_response 是 dict，则用 Pydantic 校验。
        3. 如果没有 structured_response，尝试把最后一条消息按 JSON 解析。
        4. 都失败时抛错，由节点决定是否走 fallback。
        """
        agent = self.create_structured_agent(response_model, system_prompt=system_prompt)
        # 所有节点真实打模型前都必须经过这个全局 semaphore。
        # 它解决的是“单 Python 进程瞬间打爆 LLM API”，不是 run 排队问题。
        async with self._llm_semaphore:
            result = await agent.ainvoke({"messages": messages})

        structured_response = result.get("structured_response")
        if structured_response is not None:
            if isinstance(structured_response, response_model):
                return structured_response
            return response_model.model_validate(structured_response)

        messages_result = result.get("messages") or []
        if messages_result:
            content = getattr(messages_result[-1], "content", None)
            if isinstance(content, str) and content.strip():
                return response_model.model_validate_json(content)

        raise ValueError(f"Agent 未返回 {response_model.__name__} 结构化结果")

def build_provider_headers(provider: str, api_key: str) -> dict[str, str] | None:
    """按模型厂商补充 OpenAI 兼容接口的特殊 header。"""
    if provider.lower() == "mimo":
        return {"api-key": api_key}
    return None


agent_factory = AgentFactory()
