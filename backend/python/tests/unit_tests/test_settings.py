"""配置加载的冒烟测试：保证「没配 .env」时服务仍然起得来。

背景：deepseek_api_key / llm_api_key 曾用 `os.getenv("XXX")` 当默认值。
环境变量不存在时 os.getenv 返回 None，与声明的 str 类型冲突，
pydantic 会在构造 Settings 时直接抛 ValidationError。

后果：
1. CI（没有 .env）一跑 pytest 就收集失败；
2. 桌面端用户第一次启动、还没填 .env 时，Python 后端直接起不来。

这里锁死「缺 key 时是空串、不是 None」。
"""
import os

from src.app.config.settings import Settings


def test_settings_loads_without_any_api_key(monkeypatch):
    """不提供任何 API key 时，Settings 仍应能构造，且 key 是空串而不是 None。"""
    monkeypatch.delenv("DEEPSEEK_API_KEY", raising=False)
    monkeypatch.delenv("MIMO_API_KEY", raising=False)

    # _env_file=None 跳过 .env 读取，模拟「用户还没建 .env」的全新环境。
    settings = Settings(_env_file=None)

    assert settings.deepseek_api_key == ""
    assert settings.llm_api_key == ""
    assert settings.effective_llm_api_key == ""


def test_settings_reads_keys_from_environment(monkeypatch):
    """环境变量存在时，key 仍应被正确读入。"""
    monkeypatch.setenv("DEEPSEEK_API_KEY", "sk-test-value")
    monkeypatch.setenv("MIMO_API_KEY", "tp-test-value")

    settings = Settings(_env_file=None)

    assert settings.deepseek_api_key == "sk-test-value"
    assert settings.llm_api_key == "tp-test-value"


def test_effective_key_follows_provider(monkeypatch):
    """effective_llm_api_key 要按 LLM_PROVIDER 选对那一个。

    注意：init kwargs 必须用 alias 名（LLM_PROVIDER）而不是字段名（llm_provider）——
    pydantic-settings 默认 populate_by_name=False，用字段名传会被静默忽略。
    """
    monkeypatch.setenv("DEEPSEEK_API_KEY", "sk-deepseek")
    monkeypatch.setenv("MIMO_API_KEY", "tp-mimo")

    deepseek = Settings(_env_file=None, LLM_PROVIDER="deepseek")
    mimo = Settings(_env_file=None, LLM_PROVIDER="mimo")

    assert deepseek.llm_provider == "deepseek"
    assert deepseek.effective_llm_api_key == "sk-deepseek"
    assert mimo.llm_provider == "mimo"
    assert mimo.effective_llm_api_key == "tp-mimo"


def test_no_env_file_and_no_env_vars_is_importable():
    """最严苛的场景：既没有 .env 也没有任何环境变量，模块仍可导入。"""
    for key in ("DEEPSEEK_API_KEY", "MIMO_API_KEY", "LLM_PROVIDER"):
        os.environ.pop(key, None)

    settings = Settings(_env_file=None)

    assert settings.llm_provider
    assert settings.llm_base_url
    assert settings.llm_chat_model
