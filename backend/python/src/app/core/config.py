from functools import lru_cache
from typing import Any

from dotenv import load_dotenv
from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict

load_dotenv()


class Settings(BaseSettings):
    """统一管理 Python 服务运行所需的环境配置。

    Attributes:
        llm_provider: 设置页使用的服务预设标识。
        llm_model_provider: LangChain ChatModel 集成标识。
        llm_api_key: 模型服务鉴权密钥。
        llm_base_url: 模型服务或中转站 API 地址。
        llm_chat_model: 服务商提供的模型 ID。
        llm_provider_options: 所选集成的原生初始化参数。
    """

    # 统一从 .env 读取配置，未声明的额外字段直接忽略。
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    # 应用基础配置。
    app_name: str = "AI Resume Forge Python API"
    app_env: str = "dev"
    app_debug: bool = False

    # 所有 AI 功能共用一套 LangChain ChatModel 配置。
    # default 必须是空串而不是 os.getenv(...):
    #   - os.getenv 在类定义时求值，key 会被冻结进字段 default（还可能把 .env 的真实 key
    #     带进 model_json_schema / repr）；
    #   - 环境变量不存在时 os.getenv 返回 None，与 str 类型冲突，pydantic 直接校验失败。
    # pydantic-settings 自身会按 init > 环境变量 > .env > default 的顺序取值。
    llm_provider: str = Field(default="deepseek", alias="LLM_PROVIDER")
    llm_model_provider: str = Field(default="deepseek", alias="LLM_MODEL_PROVIDER")
    llm_api_key: str = Field(default="", alias="LLM_API_KEY")
    llm_base_url: str = Field(default="https://api.deepseek.com", alias="LLM_BASE_URL")
    llm_chat_model: str = Field(default="deepseek-flash", alias="LLM_MODEL")
    llm_max_tokens: int = Field(default=4096, alias="LLM_MAX_TOKENS")
    llm_extra_headers: dict[str, str] = Field(default_factory=dict, alias="LLM_EXTRA_HEADERS")
    llm_provider_options: dict[str, Any] = Field(default_factory=dict, alias="LLM_PROVIDER_OPTIONS")
    llm_timeout_seconds: int = Field(default=60, alias="LLM_TIMEOUT_SECONDS")

    @property
    def effective_llm_api_key(self) -> str:
        """返回当前统一模型密钥。

        Returns:
            当前配置的 API Key。
        """
        return self.llm_api_key

    @property
    def effective_llm_base_url(self) -> str:
        """返回当前统一模型端点。

        Returns:
            当前配置的 Base URL。
        """
        return self.llm_base_url

    @property
    def effective_llm_chat_model(self) -> str:
        """返回当前统一模型 ID。

        Returns:
            当前配置的模型 ID。
        """
        return self.llm_chat_model

    # 是否用 LLM 给优化结果做 rubric 打分(评测系统用);false 时只跑确定性规则。
    agent_quality_llm_scoring: bool = Field(default=True, alias="AGENT_QUALITY_LLM_SCORING")

    # Agent 与 Java 内部接口配置。
    java_internal_base_url: str = Field(
        default="http://localhost:8080",
        alias="JAVA_INTERNAL_BASE_URL",
    )
    internal_agent_service_token: str = Field(
        default="",
        alias="INTERNAL_AGENT_SERVICE_TOKEN",
    )
    # LangGraph checkpoint 存储：SQLite 文件，桌面端不需要再装 Redis。
    checkpoint_db_path: str = Field(
        default="./data/checkpoints.db",
        alias="CHECKPOINT_DB_PATH",
    )
    # Agent 任务队列用的 SQLite 文件，必须和 Java 后端指向同一个库。
    agent_task_db_path: str = Field(
        default="../java/data/ai_resume.db",
        alias="AGENT_TASK_DB_PATH",
    )
    agent_checkpoint_ttl_seconds: int = Field(
        default=3600,
        alias="AGENT_CHECKPOINT_TTL_SECONDS",
    )
    agent_summary_trigger_message_count: int = Field(default=12, alias="AGENT_SUMMARY_TRIGGER_MESSAGE_COUNT")
    agent_summary_trigger_context_ratio: float = Field(default=0.9, alias="AGENT_SUMMARY_TRIGGER_CONTEXT_RATIO")
    agent_model_context_length: int = Field(default=196608, alias="AGENT_MODEL_CONTEXT_LENGTH")
    agent_summary_keep_recent_messages: int = Field(default=6, alias="AGENT_SUMMARY_KEEP_RECENT_MESSAGES")
    agent_review_max_retry: int = Field(default=2, alias="AGENT_REVIEW_MAX_RETRY")
    agent_event_flush_size: int = Field(default=10, alias="AGENT_EVENT_FLUSH_SIZE")

    # SQLite 任务队列配置。
    # Java 负责投递消息，Python worker 负责消费；这组名字必须和 Java agent.stream.* 对齐。
    # 原来 RabbitMQ 的 exchange + routingKey 换成了「一个 stream 一个消费组」，
    # START / CONTINUE 的区分放进消息体的 jobType 字段。
    agent_resume_run_stream: str = Field(default="agent.resume.run", alias="AGENT_RESUME_RUN_STREAM")
    agent_interview_run_stream: str = Field(default="agent.interview.run", alias="AGENT_INTERVIEW_RUN_STREAM")
    agent_run_result_stream: str = Field(default="agent.run.result", alias="AGENT_RUN_RESULT_STREAM")

    # 死信流，接收 JSON 格式错误、字段不合法等 worker 无法理解的消息，方便手动排查。
    agent_run_dead_letter_stream: str = Field(default="agent.run.dlq", alias="AGENT_RUN_DLQ_STREAM")

    # 消费组：resume / interview 各自一个组；Java 侧消费回写结果用的是第三个组。
    agent_resume_worker_group: str = Field(default="agent-resume-worker", alias="AGENT_RESUME_WORKER_GROUP")
    agent_interview_worker_group: str = Field(default="agent-interview-worker", alias="AGENT_INTERVIEW_WORKER_GROUP")
    agent_result_consumer_group: str = Field(default="java-result-consumer", alias="AGENT_RESULT_CONSUMER_GROUP")
    agent_dlq_consumer_group: str = Field(default="java-dlq-reader", alias="AGENT_DLQ_CONSUMER_GROUP")

    # 任务队列空轮询间隔（秒）。
    # 桌面端把 worker 挂在 FastAPI 的 lifespan 里，轮询循环必须周期性让出
    # event loop，否则队列空闲时会把 HTTP 服务饿死。
    agent_task_poll_interval_seconds: float = Field(default=0.5, alias="AGENT_TASK_POLL_INTERVAL_SECONDS")

    # 认领超时（秒）：CLAIMED 状态超过该时长仍未完成的消息视为 worker 崩溃残留，
    # 由回收逻辑放回 PENDING 重新消费。必须远大于最长的单次 run 耗时（含 LLM 慢调用），
    # 否则会把正在执行的消息误回收导致重复执行（重复执行会被 Java claim_run 挡住，但仍应避免）。
    agent_task_claim_timeout_seconds: int = Field(
        default=1800,
        alias="AGENT_TASK_CLAIM_TIMEOUT_SECONDS",
    )

    java_internal_timeout_seconds: int = Field(
        default=30,
        alias="JAVA_INTERNAL_TIMEOUT_SECONDS",
    )


@lru_cache
def get_settings() -> Settings:
    """返回配置单例，避免重复解析环境变量。

    Returns:
        当前进程共享的 Settings 实例。
    """
    return Settings()
