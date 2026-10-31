/**
 * 桌面端应用配置：读写安装目录下的 config/application.yaml。
 *
 * 设计约束：
 * 1. 配置是**应用级**的（一个安装目录一份），不是用户级，所以不入库。
 * 2. 存 yml 而不是 SQLite：人可读、可手改、可用 diff 比对。
 * 3. 文件位置 = 安装目录（用户装到哪就放哪），不是 %APPDATA%。
 */
import fs from 'node:fs';
import path from 'node:path';
import { parse, stringify } from 'yaml';

/** 全部 Python AI 功能共用的模型配置。 */
export interface AiModelConfig {
  /** 供应商预设标识；custom 表示用户自定义端点。 */
  provider: string;
  /** LangChain ChatModel 集成标识，例如 openai、deepseek、ollama。 */
  modelProvider: string;
  /** 供应商兼容 API 的 Base URL。 */
  baseUrl: string;
  apiKey: string;
  model: string;
  maxTokens: number;
  extraHeaders: Record<string, string>;
  providerOptions: Record<string, unknown>;
}

/** 整体应用配置 */
export interface AppConfig {
  agent: AiModelConfig;
  /** Java 后端监听地址 */
  server: {
    host: string;
    port: number;
  };
  /** Python AI 运行时监听地址 */
  python: {
    host: string;
    port: number;
  };
  /** 内部接口令牌，Java 与 Python 必须一致 */
  internalToken: string;
}

/** 配置目录名（相对安装目录） */
export const CONFIG_DIR_NAME = 'config';

/** 配置文件名（相对安装目录） */
export const CONFIG_FILE_NAME = 'application.yaml';

export function defaultConfig(): AppConfig {
  return {
    agent: {
      provider: 'deepseek',
      modelProvider: 'deepseek',
      baseUrl: 'https://api.deepseek.com',
      apiKey: '',
      model: 'deepseek-flash',
      maxTokens: 4096,
      extraHeaders: {},
      providerOptions: {},
    },
    server: {
      host: '127.0.0.1',
      port: 8080,
    },
    python: {
      host: '127.0.0.1',
      port: 8000,
    },
    internalToken:
      // 这个值同时用作「Java ↔ Python 内部通信令牌」和「JWT 签名密钥」
      // （backend_process.ts 里 JWT_SECRET: config.internalToken）。
      // 默认值原来只有 9 字节，HS256 要求至少 256 bit —— 短密钥会让
      // 注册接口直接失败。现在给一个足够长的默认值，用户可以在设置界面改。
      'ai-resume-forge-internal-token-please-change-me-2026',
  };
}

/** 配置文件绝对路径 */
export function configFilePath(installDir: string): string {
  return path.join(installDir, CONFIG_DIR_NAME, CONFIG_FILE_NAME);
}

/**
 * 读取配置；文件不存在时返回默认值并把默认配置落盘。
 * 解析失败时回退到默认值，绝不因为配置文件写坏就让桌面端起不来。
 */
export function loadConfig(installDir: string): AppConfig {
  const file = configFilePath(installDir);
  if (!fs.existsSync(file)) {
    const config = defaultConfig();
    saveConfig(installDir, config);
    return config;
  }
  try {
    const parsed = parse(fs.readFileSync(file, 'utf-8')) as LegacyAppConfig | null;
    return mergeConfig(defaultConfig(), parsed ?? {});
  } catch (error) {
    console.error(`[config] 解析 ${file} 失败，回退到默认配置:`, error);
    return defaultConfig();
  }
}

/** 保存配置（写入前确保目录存在） */
export function saveConfig(installDir: string, config: AppConfig): void {
  const file = configFilePath(installDir);
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, stringify(config, { indent: 2, lineWidth: 120 }), 'utf-8');
}

/** 用已有配置逐字段覆盖默认值，缺字段补默认 */
function mergeConfig(base: AppConfig, override: LegacyAppConfig): AppConfig {
  let agentOverride = override.agent ?? {};
  const legacyChat = override.chat;
  if (!agentOverride.apiKey?.trim() && legacyChat?.apiKey?.trim()) {
    const baseUrl = legacyChat.baseUrl ?? '';
    const modelProvider = /\/anthropic(?:\/|$)/i.test(baseUrl) ? 'anthropic' : 'openai';
    agentOverride = {
      provider: modelProvider === 'anthropic' && /minimaxi/i.test(baseUrl) ? 'minimax' : 'custom',
      modelProvider,
      baseUrl,
      apiKey: legacyChat.apiKey,
      model: legacyChat.model ?? '',
      maxTokens: legacyChat.maxTokens ?? base.agent.maxTokens,
    };
  } else if (!agentOverride.modelProvider && agentOverride.provider) {
    agentOverride = {
      ...agentOverride,
      modelProvider: agentOverride.provider === 'deepseek' ? 'deepseek' : 'openai',
    };
  }
  return {
    agent: { ...base.agent, ...agentOverride },
    server: { ...base.server, ...(override.server ?? {}) },
    python: { ...base.python, ...(override.python ?? {}) },
    internalToken: override.internalToken ?? base.internalToken,
  };
}

type LegacyChatModelConfig = {
  baseUrl?: string;
  apiKey?: string;
  model?: string;
  maxTokens?: number;
};

type LegacyAppConfig = Partial<AppConfig> & {
  chat?: LegacyChatModelConfig;
};
