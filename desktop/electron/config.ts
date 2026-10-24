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

/** Java 对话模型配置（Anthropic 兼容端点） */
export interface ChatModelConfig {
  /** Anthropic 兼容端点前缀，实际请求 {baseUrl}/v1/messages */
  baseUrl: string;
  /** API Key，同时作为 x-api-key 和 authorization 头 */
  apiKey: string;
  /** 模型名 */
  model: string;
  /** 单次请求最大输出 token */
  maxTokens: number;
}

/** Python Agent 模型配置（OpenAI 兼容端点） */
export interface AgentModelConfig {
  /** 供应商：deepseek / mimo / custom */
  provider: string;
  /** OpenAI 兼容端点前缀 */
  baseUrl: string;
  apiKey: string;
  model: string;
}

/** 整体应用配置 */
export interface AppConfig {
  chat: ChatModelConfig;
  agent: AgentModelConfig;
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
    chat: {
      baseUrl: 'https://api.minimaxi.com/anthropic',
      apiKey: '',
      model: 'MiniMax-M3.1-Flash',
      maxTokens: 4096,
    },
    agent: {
      provider: 'deepseek',
      baseUrl: 'https://api.deepseek.com/v1',
      apiKey: '',
      model: 'deepseek-chat',
    },
    server: {
      host: '127.0.0.1',
      port: 8080,
    },
    python: {
      host: '127.0.0.1',
      port: 8000,
    },
    internalToken: 'change-me',
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
    const parsed = parse(fs.readFileSync(file, 'utf-8')) as Partial<AppConfig> | null;
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
function mergeConfig(base: AppConfig, override: Partial<AppConfig>): AppConfig {
  return {
    chat: { ...base.chat, ...(override.chat ?? {}) },
    agent: { ...base.agent, ...(override.agent ?? {}) },
    server: { ...base.server, ...(override.server ?? {}) },
    python: { ...base.python, ...(override.python ?? {}) },
    internalToken: override.internalToken ?? base.internalToken,
  };
}
