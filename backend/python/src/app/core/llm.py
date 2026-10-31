from typing import TypeVar

from langchain.agents import create_agent
from langchain.chat_models import init_chat_model
from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import BaseMessage
from pydantic import BaseModel

from app.core.config import get_settings

StructuredModel = TypeVar("StructuredModel", bound=BaseModel)


class AgentFactory:
    """负责创建和调用 LangChain Agent。

    使用方式：
    1. 各 LangGraph 节点准备 messages 和 response_model。
    2. invoke_agent 创建结构化 Agent。
    3. LangChain 调用底层 ChatModel。
    4. 返回 Pydantic 结构化结果，节点再写入 state。
    """

    def create_chat_model(self) -> BaseChatModel:
        """创建底层 ChatModel，供 create_agent 使用。

        配置来源：
        1. LLM API key / base_url / model 从 settings 读取。
        2. temperature 固定较低，减少结构化输出波动。
        3. 需要 API Key 的集成未配置 key 时抛错，让节点走本地兜底逻辑。

        Returns:
            配置完成的 LangChain BaseChatModel。
        """
        # 1. 读取统一配置，并检查当前集成所需的凭据。
        settings = get_settings()
        api_key = settings.effective_llm_api_key
        model_provider = settings.llm_model_provider.strip().lower()
        provider_options = dict(settings.llm_provider_options)
        keyless_providers = {"ollama", "bedrock", "huggingface", "google_genai", "litellm"}
        if (not api_key or api_key.startswith("your_")) and model_provider not in keyless_providers:
            raise ValueError("LLM API key 未配置，跳过真实 Agent 调用")
        headers = build_provider_headers(settings.llm_provider, api_key) or {}
        for name, value in settings.llm_extra_headers.items():
            for existing_name in tuple(headers):
                if existing_name.casefold() == name.casefold():
                    del headers[existing_name]
            headers[name] = value
        kwargs = provider_options
        kwargs.setdefault("temperature", 0.2)

        # 2. 将统一字段映射到集成参数名，并允许原生参数覆盖默认映射。
        api_key_arg = {
            "huggingface": "huggingfacehub_api_token",
            "mistralai": "mistral_api_key",
        }.get(model_provider, "api_key")
        if api_key and model_provider != "ollama":
            kwargs.setdefault(api_key_arg, api_key)

        base_url_arg = {
            "huggingface": "endpoint_url",
            "litellm": "api_base",
            "mistralai": "endpoint",
            "bedrock": "endpoint_url",
        }.get(model_provider)
        if settings.effective_llm_base_url:
            if model_provider == "google_genai":
                client_options = dict(kwargs.get("client_options") or {})
                client_options.setdefault("api_endpoint", settings.effective_llm_base_url)
                kwargs["client_options"] = client_options
            else:
                kwargs.setdefault(base_url_arg or "base_url", settings.effective_llm_base_url)

        max_tokens_arg = {
            "google_genai": "max_output_tokens",
            "huggingface": "max_new_tokens",
            "ollama": "num_predict",
        }.get(model_provider, "max_tokens")
        if settings.llm_max_tokens > 0:
            kwargs.setdefault(max_tokens_arg, settings.llm_max_tokens)

        if settings.llm_timeout_seconds > 0:
            if model_provider == "litellm":
                kwargs.setdefault("request_timeout", settings.llm_timeout_seconds)
            elif model_provider == "openrouter":
                # langchain-openrouter 的 timeout 单位是毫秒。
                kwargs.setdefault("timeout", settings.llm_timeout_seconds * 1000)
            elif model_provider in {
                "openai", "anthropic", "deepseek", "google_genai", "xai", "mistralai", "groq", "together",
                "huggingface", "bedrock"
            }:
                kwargs.setdefault("timeout", settings.llm_timeout_seconds)

        if headers:
            headers_arg = {
                "google_genai": "additional_headers",
                "litellm": "extra_headers",
            }.get(model_provider)
            if model_provider == "ollama":
                for client_arg in ("client_kwargs", "async_client_kwargs"):
                    client_options = dict(kwargs.get(client_arg) or {})
                    client_headers = dict(client_options.get("headers") or {})
                    for name, value in headers.items():
                        for existing_name in tuple(client_headers):
                            if existing_name.casefold() == name.casefold():
                                del client_headers[existing_name]
                        client_headers[name] = value
                    client_options["headers"] = client_headers
                    kwargs[client_arg] = client_options
            elif headers_arg:
                configured_headers = dict(kwargs.get(headers_arg) or {})
                for name, value in headers.items():
                    for existing_name in tuple(configured_headers):
                        if existing_name.casefold() == name.casefold():
                            del configured_headers[existing_name]
                    configured_headers[name] = value
                kwargs[headers_arg] = configured_headers
            elif model_provider in {"openai", "anthropic", "deepseek", "openrouter", "xai", "groq", "together"}:
                configured_headers = dict(kwargs.get("default_headers") or {})
                for name, value in headers.items():
                    for existing_name in tuple(configured_headers):
                        if existing_name.casefold() == name.casefold():
                            del configured_headers[existing_name]
                    configured_headers[name] = value
                kwargs["default_headers"] = configured_headers
            else:
                raise ValueError(f"{model_provider} 集成不支持通用额外请求头，请通过该集成的 providerOptions 配置")

        if model_provider == "huggingface":
            # ChatHuggingFace 默认尝试本地 pipeline；桌面端更常见的是已部署的推理端点。
            kwargs.setdefault("backend", "endpoint")

        kwargs.pop("model", None)
        kwargs.pop("model_provider", None)
        # 3. 交给 LangChain provider factory 延迟加载对应集成。
        return init_chat_model(
            model=settings.effective_llm_chat_model,
            model_provider=model_provider,
            **kwargs,
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

        Args:
            response_model: Agent 输出的 Pydantic 结构类型。
            system_prompt: 可选系统提示词。

        Returns:
            使用统一 ChatModel 的 LangChain Agent。
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

        Args:
            messages: 当前节点交给模型的消息列表。
            response_model: 模型输出对应的 Pydantic 类型。
            system_prompt: 可选的系统提示词。

        Returns:
            校验通过的结构化模型结果。
        """
        agent = self.create_structured_agent(response_model, system_prompt=system_prompt)
        # 1. 模型请求直接并发执行，不通过应用层信号量排队。
        result = await agent.ainvoke({"messages": messages})

        # 2. 优先返回 LangChain 生成的结构化结果。
        structured_response = result.get("structured_response")
        if structured_response is not None:
            if isinstance(structured_response, response_model):
                return structured_response
            return response_model.model_validate(structured_response)

        # 3. 兼容 structured_response 缺失时从最后一条消息解析 JSON。
        messages_result = result.get("messages") or []
        if messages_result:
            content = getattr(messages_result[-1], "content", None)
            if isinstance(content, str) and content.strip():
                return response_model.model_validate_json(content)

        raise ValueError(f"Agent 未返回 {response_model.__name__} 结构化结果")

def build_provider_headers(provider: str, api_key: str) -> dict[str, str] | None:
    """为需要非标准认证头的供应商返回默认请求头。

    Args:
        provider: 设置页选中的服务预设。
        api_key: 用户填写的模型服务密钥。

    Returns:
        需要补充的请求头；无需补充时返回 None。
    """
    if provider.lower() == "mimo":
        return {"api-key": api_key}
    return None


agent_factory = AgentFactory()
