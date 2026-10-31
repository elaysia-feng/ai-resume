"""配置加载的冒烟测试：保证「没配 .env」时服务仍然起得来。

背景：模型配置曾按供应商维护多套 Key 和 Base URL，切换预设时容易读取到另一组凭据。

后果：
1. CI（没有 .env）一跑 pytest 就收集失败；
2. 桌面端用户第一次启动、还没填 .env 时，Python 后端直接起不来。

这里锁死「缺 key 时是空串、不是 None」。
"""
import os

from app.core.config import Settings


def test_settings_loads_without_any_api_key(monkeypatch):
    """不提供任何 API key 时，Settings 仍应能构造，且 key 是空串而不是 None。

    Args:
        monkeypatch: pytest 环境变量隔离工具。
    """
    monkeypatch.delenv("LLM_API_KEY", raising=False)

    # _env_file=None 跳过 .env 读取，模拟「用户还没建 .env」的全新环境。
    settings = Settings(_env_file=None)

    assert settings.llm_api_key == ""
    assert settings.effective_llm_api_key == ""


def test_settings_reads_keys_from_environment(monkeypatch):
    """环境变量存在时，key 仍应被正确读入。

    Args:
        monkeypatch: pytest 环境变量隔离工具。
    """
    monkeypatch.setenv("LLM_API_KEY", "sk-test-value")
    monkeypatch.setenv("LLM_BASE_URL", "https://example.com/v1")
    monkeypatch.setenv("LLM_MODEL", "example-model")

    settings = Settings(_env_file=None)

    assert settings.llm_api_key == "sk-test-value"
    assert settings.llm_base_url == "https://example.com/v1"
    assert settings.llm_chat_model == "example-model"


def test_chatmodel_settings_are_shared_across_provider_presets(monkeypatch):
    """切换供应商预设时，Python 仍使用同一组模型凭据配置。

    注意：init kwargs 必须用 alias 名（LLM_PROVIDER）而不是字段名（llm_provider）——
    pydantic-settings 默认 populate_by_name=False，用字段名传会被静默忽略。

    Args:
        monkeypatch: pytest 环境变量隔离工具。
    """
    monkeypatch.setenv("LLM_API_KEY", "sk-user-key")

    deepseek = Settings(_env_file=None, LLM_PROVIDER="deepseek")
    zhipu = Settings(_env_file=None, LLM_PROVIDER="zhipu")

    assert deepseek.llm_provider == "deepseek"
    assert deepseek.effective_llm_api_key == "sk-user-key"
    assert zhipu.llm_provider == "zhipu"
    assert zhipu.effective_llm_api_key == "sk-user-key"


def test_no_env_file_and_no_env_vars_is_importable():
    """最严苛的场景：既没有 .env 也没有任何环境变量，模块仍可导入。"""
    for key in ("LLM_API_KEY", "LLM_PROVIDER", "LLM_MODEL_PROVIDER", "LLM_PROVIDER_OPTIONS"):
        os.environ.pop(key, None)

    settings = Settings(_env_file=None)

    assert settings.llm_provider
    assert settings.llm_base_url
    assert settings.llm_chat_model
