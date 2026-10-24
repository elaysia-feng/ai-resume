/**
 * 后端进程管理：桌面端拉起并守护 Java 后端与 Python AI 运行时。
 *
 * 桌面端是自包含应用：用户装完就能用，不需要自己开命令行起后端。
 * 主进程在这里负责三个后端进程的完整生命周期：
 * 1. 启动：按配置注入环境变量后 spawn。
 * 2. 守护：进程意外退出时按退避间隔自动重启，避免用户看到"后端未启动"。
 * 3. 收尾：桌面端退出时把子进程一起带走，不留孤儿进程。
 */
import { ChildProcess, spawn } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import type { AppConfig } from './config';

/** 单个后端进程的运行状态 */
export interface BackendProcessState {
  /** 进程标识：java / python */
  id: 'java' | 'python';
  /** 是否正在运行 */
  running: boolean;
  /** 已重启次数 */
  restarts: number;
  /** 最近一次退出码，null 表示还没退出过 */
  lastExitCode: number | null;
  /** 最近一次退出原因 */
  lastExitReason: string;
}

/** 进程日志落盘位置（相对安装目录） */
const LOG_DIR_NAME = 'logs';

/** 自动重启的退避间隔（毫秒），按重启次数递增 */
const RESTART_DELAYS_MS = [1000, 2000, 5000, 10000, 30000];

/** 单个后端进程的管理器 */
class ManagedProcess {
  private child: ChildProcess | null = null;
  private timer: NodeJS.Timeout | null = null;
  private stopping = false;
  /** 已自动重启次数，用于退避间隔和状态展示 */
  private restarts = 0;

  constructor(
    readonly id: 'java' | 'python',
    private readonly resolveCommand: () => { command: string; args: string[]; env: NodeJS.ProcessEnv } | null,
    private readonly logDir: string,
    private readonly onStateChange: (state: BackendProcessState) => void,
  ) {}

  get running(): boolean {
    return this.child !== null && this.child.exitCode === null;
  }

  /** 启动进程；已在运行时直接返回 */
  start(config: AppConfig): void {
    if (this.running || this.stopping) {
      return;
    }
    const resolved = this.resolveCommand();
    if (!resolved) {
      this.onStateChange({
        id: this.id,
        running: false,
        restarts: 0,
        lastExitCode: null,
        lastExitReason: '未找到可执行文件',
      });
      return;
    }
    this.stopping = false;
    const child = spawn(resolved.command, resolved.args, {
      env: { ...process.env, ...resolved.env },
      cwd: path.dirname(resolved.command),
      windowsHide: true,
    });
    this.child = child;
    this.pipeLog(child, 'stdout');
    this.pipeLog(child, 'stderr');

    child.on('exit', (code, signal) => {
      this.child = null;
      const reason = signal ? `信号 ${signal}` : `退出码 ${code}`;
      this.onStateChange({
        id: this.id,
        running: false,
        restarts: this.restarts,
        lastExitCode: code,
        lastExitReason: reason,
      });
      if (!this.stopping) {
        this.scheduleRestart(config);
      }
    });
  }

  /** 停止进程（不再自动重启） */
  stop(): void {
    this.stopping = true;
    if (this.timer) {
      clearTimeout(this.timer);
      this.timer = null;
    }
    if (this.child) {
      this.child.kill();
      this.child = null;
    }
  }

  /** 进程退出后按退避间隔自动重启 */
  private scheduleRestart(config: AppConfig): void {
    if (this.timer) {
      clearTimeout(this.timer);
    }
    const delay = RESTART_DELAYS_MS[Math.min(this.restarts, RESTART_DELAYS_MS.length - 1)];
    this.restarts += 1;
    this.timer = setTimeout(() => {
      this.timer = null;
      this.start(config);
    }, delay);
  }

  /** 把子进程输出写到日志文件 */
  private pipeLog(child: ChildProcess, stream: 'stdout' | 'stderr'): void {
    const source = child[stream];
    if (!source) {
      return;
    }
    try {
      fs.mkdirSync(this.logDir, { recursive: true });
      const file = path.join(this.logDir, `${this.id}.log`);
      const out = fs.createWriteStream(file, { flags: 'a' });
      source.on('data', (chunk: Buffer) => {
        out.write(chunk);
      });
      source.on('end', () => out.end());
    } catch (error) {
      console.error(`[backend] 写 ${this.id} 日志失败:`, error);
    }
  }

  get restartCount(): number {
    return this.restarts;
  }
}
/**
 * 后端进程总管：统一管理 Java 与 Python 两个进程。
 */
export class BackendSupervisor {
  private readonly java: ManagedProcess;
  private readonly python: ManagedProcess;
  private readonly states = new Map<'java' | 'python', BackendProcessState>();
  private config: AppConfig;

  constructor(
    private readonly installDir: string,
    config: AppConfig,
    private readonly onStateChange?: (state: BackendProcessState) => void,
  ) {
    this.config = config;
    const logDir = path.join(installDir, LOG_DIR_NAME);
    this.java = new ManagedProcess('java', () => this.javaCommand(config), logDir, (state) => this.emit(state));
    this.python = new ManagedProcess('python', () => this.pythonCommand(config), logDir, (state) => this.emit(state));
    this.states.set('java', { id: 'java', running: false, restarts: 0, lastExitCode: null, lastExitReason: '' });
    this.states.set('python', { id: 'python', running: false, restarts: 0, lastExitCode: null, lastExitReason: '' });
  }

  /** 启动两个后端 */
  start(config?: AppConfig): void {
    if (config) {
      this.config = config;
    }
    this.java.start(this.config);
    this.python.start(this.config);
  }

  /** 停止两个后端（不再自动重启） */
  stop(): void {
    this.java.stop();
    this.python.stop();
  }

  /** 重启两个后端（设置界面改完配置后调用） */
  restart(config?: AppConfig): void {
    if (config) {
      this.config = config;
    }
    this.java.stop();
    this.python.stop();
    // 停掉后立刻重启：stop() 把 stopping 置位，这里需要复位。
    setTimeout(() => this.start(this.config), 500);
  }

  /** 当前状态快照 */
  status(): BackendProcessState[] {
    return [...this.states.values()].map((state) => ({
      ...state,
      running: state.id === 'java' ? this.java.running : this.python.running,
    }));
  }

  private emit(state: BackendProcessState): void {
    this.states.set(state.id, state);
    this.onStateChange?.(state);
  }

  /**
   * Java 后端启动命令。
   *
   * 打包形态：resources/backend/ai-resume-backend/ai-resume-backend.exe
   * （jpackage app-image，自带 JRE，用户机器上不需要装 Java）
   *
   * 开发形态：直接用系统 java 跑 jar。
   */
  private javaCommand(config: AppConfig): { command: string; args: string[]; env: NodeJS.ProcessEnv } | null {
    const packaged = path.join(this.installDir, 'resources', 'backend', 'ai-resume-backend', 'ai-resume-backend.exe');
    const configDir = path.join(this.installDir, 'config');
    const dataDir = path.join(this.installDir, 'data');
    const env: NodeJS.ProcessEnv = {
      DB_FILE: path.join(dataDir, 'ai_resume.db'),
      JWT_SECRET: config.internalToken,
      INTERNAL_AGENT_SERVICE_TOKEN: config.internalToken,
      PYTHON_AGENT_BASE_URL: `http://${config.python.host}:${config.python.port}`,
      MINIMAX_BASE_URL: config.chat.baseUrl,
      MINIMAX_ANTHROPIC_API_KEY: config.chat.apiKey,
      MINIMAX_MODEL: config.chat.model,
      MINIMAX_MAX_TOKENS: String(config.chat.maxTokens),
      STORAGE_ROOT: path.join(dataDir, 'storage'),
    };

    if (fs.existsSync(packaged)) {
      return {
        command: packaged,
        // --spring.config.additional-location 让外部 config/application.yaml 覆盖 jar 内默认值
        args: [`--spring.config.additional-location=file:${configDir}${path.sep}`],
        env,
      };
    }

    // 开发形态：仓库里的 jar + 系统 java
    const repoRoot = path.resolve(this.installDir, '..', '..');
    const jar = path.join(repoRoot, 'backend', 'java', 'target', 'ai-resume-forge-backend.jar');
    if (!fs.existsSync(jar)) {
      return null;
    }
    return {
      command: 'java',
      args: ['-jar', jar, `--spring.config.additional-location=file:${configDir}${path.sep}`],
      env,
    };
  }

  /**
   * Python AI 运行时启动命令。
   *
   * 打包形态：resources/python/ai-resume-api/ai-resume-api.exe（PyInstaller onedir）
   * 开发形态：仓库 .venv 里的 python -m uvicorn
   */
  private pythonCommand(config: AppConfig): { command: string; args: string[]; env: NodeJS.ProcessEnv } | null {
    const dataDir = path.join(this.installDir, 'data');
    const env: NodeJS.ProcessEnv = {
      AGENT_TASK_DB_PATH: path.join(dataDir, 'ai_resume.db'),
      CHECKPOINT_DB_PATH: path.join(dataDir, 'checkpoints.db'),
      JAVA_INTERNAL_BASE_URL: `http://${config.server.host}:${config.server.port}`,
      INTERNAL_AGENT_SERVICE_TOKEN: config.internalToken,
      LLM_PROVIDER: config.agent.provider,
      DEEPSEEK_API_KEY: config.agent.apiKey,
      MIMO_API_KEY: config.agent.apiKey,
      AI_RESUME_PYTHON_HOST: config.python.host,
      AI_RESUME_PYTHON_PORT: String(config.python.port),
    };

    const packaged = path.join(this.installDir, 'resources', 'python', 'ai-resume-api', 'ai-resume-api.exe');
    if (fs.existsSync(packaged)) {
      return { command: packaged, args: [], env };
    }

    const repoRoot = path.resolve(this.installDir, '..', '..');
    const python = path.join(repoRoot, 'backend', 'python', '.venv', 'Scripts', 'python.exe');
    if (!fs.existsSync(python)) {
      return null;
    }
    return {
      command: python,
      args: ['-m', 'uvicorn', 'src.app.main:app', '--host', config.python.host, '--port', String(config.python.port)],
      env: { ...env, PYTHONPATH: path.join(repoRoot, 'backend', 'python') },
    };
  }
}
