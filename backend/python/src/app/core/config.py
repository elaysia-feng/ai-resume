from functools import lru_cache

from dotenv import load_dotenv
from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict

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

    # 文本模型配置，后续由 LangChain ChatModel 使用。
    # llm_* 是 agent_factory 实际读取的"生效配置"；effective_* 属性按 LLM_PROVIDER 选择。
    # default 必须是空串而不是 os.getenv(...):
    #   - os.getenv 在类定义时求值，key 会被冻结进字段 default（还可能把 .env 的真实 key
    #     带进 model_json_schema / repr）；
    #   - 环境变量不存在时 os.getenv 返回 None，与 str 类型冲突，pydantic 直接校验失败。
    # pydantic-settings 自身会按 init > 环境变量 > .env > default 的顺序取值。
    llm_provider: str = Field(default="mimo", alias="LLM_PROVIDER")
    llm_api_key: str = Field(default="", alias="MIMO_API_KEY")
    llm_base_url: str = Field(
        default="https://api.xiaomimimo.com/v1",
        alias="MIMO_BASE_URL",
    )
    llm_chat_model: str = Field(
        default="mimo-v2.5-pro",
        alias="MIMO_CHAT_MODEL",
    )
    llm_timeout_seconds: int = Field(default=60, alias="LLM_TIMEOUT_SECONDS")

    # DeepSeek 配置（LLM_PROVIDER=deepseek 时生效，通过 effective_* 属性切换）。
    deepseek_api_key: str = Field(default="", alias="DEEPSEEK_API_KEY")
    deepseek_base_url: str = Field(default="https://api.deepseek.com/v1", alias="DEEPSEEK_BASE_URL")
    # deepseek-chat 为快速非推理模型（结构化输出稳定）；deepseek-v4-flash 为推理模型（更慢更贵）。
    deepseek_chat_model: str = Field(default="deepseek-chat", alias="DEEPSEEK_CHAT_MODEL")

    @property
    def effective_llm_api_key(self) -> str:
        return self.deepseek_api_key if self.llm_provider.lower() == "deepseek" else self.llm_api_key

    @property
    def effective_llm_base_url(self) -> str:
        return self.deepseek_base_url if self.llm_provider.lower() == "deepseek" else self.llm_base_url

    @property
    def effective_llm_chat_model(self) -> str:
        return self.deepseek_chat_model if self.llm_provider.lower() == "deepseek" else self.llm_chat_model

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

    # 单个 Python worker 进程的 run 并发上限。
    # 单次拉取条数和 worker semaphore 都会使用这个值，不是整个集群的全局上限。
    python_agent_max_concurrent_runs: int = Field(default=4, alias="PYTHON_AGENT_MAX_CONCURRENT_RUNS")

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