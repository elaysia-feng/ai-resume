from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict
from dotenv import load_dotenv
import os

load_dotenv()

class Settings(BaseSettings):
    """统一管理 Python 服务运行所需的环境配置。"""

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

    # MiniMax 文本模型配置，后续由 LangChain ChatModel 使用。
    minimax_api_key: str = Field(default=os.getenv("MINIMAX_API_KEY"), alias="MINIMAX_API_KEY")
    minimax_base_url: str = Field(
        default=os.getenv("MINIMAX_BASE_URL"),
        alias="MINIMAX_BASE_URL",
    )
    minimax_chat_model: str = Field(
        default="MiniMax-M2.7",
        alias="MINIMAX_CHAT_MODEL",
    )
    minimax_timeout_seconds: int = Field(default=60, alias="MINIMAX_TIMEOUT_SECONDS")

    # 本地向量模型配置，用于生成检索查询和知识库 chunk 的向量。
    embedding_model_name: str = Field(
        default="BAAI/bge-small-zh-v1.5",
        alias="EMBEDDING_MODEL_NAME",
    )
    embedding_cache_folder: str = Field(
        default=r"F:\AI\embedding_models",
        alias="EMBEDDING_CACHE_FOLDER",
    )
    embedding_device: str = Field(default="cpu", alias="EMBEDDING_DEVICE")
    embedding_normalize: bool = Field(default=True, alias="EMBEDDING_NORMALIZE")
    embedding_dimension: int = Field(default=512, alias="EMBEDDING_DIMENSION")

    # Qdrant 配置，只负责向量存储和检索，不承担业务主库职责。
    qdrant_url: str = Field(
        default="https://api.cloud.qdrant.io",
        alias="QDRANT_URL",
    )
    qdrant_api_key: str = Field(default="", alias="QDRANT_API_KEY")
    qdrant_collection_name: str = Field(
        default="resume_reference_chunks",
        alias="QDRANT_COLLECTION_NAME",
    )
    qdrant_top_k: int = Field(default=5, alias="QDRANT_TOP_K")
    qdrant_score_threshold: float = Field(default=0.35, alias="QDRANT_SCORE_THRESHOLD")

    # optimize 工作流专用配置，控制检索上下文规模。
    optimize_retrieve_top_k: int = Field(default=4, alias="OPTIMIZE_RETRIEVE_TOP_K")
    optimize_max_context_chars: int = Field(
        default=2400,
        alias="OPTIMIZE_MAX_CONTEXT_CHARS",
    )
    optimize_retrieve_timeout_seconds: float = Field(default=5.0, alias="OPTIMIZE_RETRIEVE_TIMEOUT_SECONDS")

    # Agent 与 Java 内部接口配置。
    java_internal_base_url: str = Field(
        default="http://localhost:8080",
        alias="JAVA_INTERNAL_BASE_URL",
    )
    internal_agent_service_token: str = Field(
        default="",
        alias="INTERNAL_AGENT_SERVICE_TOKEN",
    )
    redis_url: str = Field(default="redis://localhost:6379/0", alias="REDIS_URL")
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

    # RabbitMQ 任务队列配置。
    # Java 负责发布消息，Python worker 负责消费；这组名字必须和 Java agent.rabbitmq.* 对齐。
    rabbitmq_url: str = Field(default="amqp://elias:779337@localhost:5672/", alias="RABBITMQ_URL")
    agent_run_exchange: str = Field(default="agent.run.exchange", alias="AGENT_RUN_EXCHANGE")
    agent_resume_run_queue: str = Field(default="agent.resume.run.queue", alias="AGENT_RESUME_RUN_QUEUE")
    agent_resume_run_start_routing_key: str = Field(
        default="agent.resume.run.start",
        alias="AGENT_RESUME_RUN_START_ROUTING_KEY",
    )
    agent_resume_run_continue_routing_key: str = Field(
        default="agent.resume.run.continue",
        alias="AGENT_RESUME_RUN_CONTINUE_ROUTING_KEY",
    )
    agent_interview_run_queue: str = Field(default="agent.interview.run.queue", alias="AGENT_INTERVIEW_RUN_QUEUE")
    agent_interview_run_start_routing_key: str = Field(
        default="agent.interview.run.start",
        alias="AGENT_INTERVIEW_RUN_START_ROUTING_KEY",
    )
    agent_interview_run_continue_routing_key: str = Field(
        default="agent.interview.run.continue",
        alias="AGENT_INTERVIEW_RUN_CONTINUE_ROUTING_KEY",
    )

    # 死信队列配置。
    # 主要接收 JSON 格式错误、字段不合法等 worker 无法理解的消息，方便手动排查。
    agent_run_dead_letter_exchange: str = Field(default="agent.run.dlx", alias="AGENT_RUN_DLX")
    agent_run_dead_letter_queue: str = Field(default="agent.run.dlq", alias="AGENT_RUN_DLQ")
    agent_run_dead_routing_key: str = Field(default="agent.run.dead", alias="AGENT_RUN_DEAD_ROUTING_KEY")

    # Python→Java 回写队列，消费 Java 侧 agent.run.result 队列。
    agent_run_result_queue: str = Field(default="agent.run.result", alias="AGENT_RUN_RESULT_QUEUE")

    # 单个 Python worker 进程的 run 并发上限。
    # RabbitMQ prefetch 和 worker semaphore 都会使用这个值，不是整个集群的全局上限。
    python_agent_max_concurrent_runs: int = Field(default=4, alias="PYTHON_AGENT_MAX_CONCURRENT_RUNS")

    # 单个 Python 进程内同时调用 LLM 的上限。
    # 一个 run 里可能有多个节点调用模型，所以它和 run 并发是两层不同的保护。
    agent_llm_max_concurrent_calls: int = Field(default=8, alias="AGENT_LLM_MAX_CONCURRENT_CALLS")
    java_internal_timeout_seconds: int = Field(
        default=30,
        alias="JAVA_INTERNAL_TIMEOUT_SECONDS",
    )


@lru_cache
def get_settings() -> Settings:
    """返回配置单例，避免重复解析环境变量。"""
    return Settings()
