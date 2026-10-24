import { net } from 'electron';
import type { BackendStatus } from './types';

/** 探活超时（毫秒） */
const PROBE_TIMEOUT_MS = 3000;

/** 探活路径：后端存在该接口且需要鉴权，返回 401 同样说明服务进程已启动 */
const PROBE_PATH = '/api/auth/me';

/**
 * 探测后端是否可达。
 * 收到任意 HTTP 响应即视为在线（401/403 说明服务在跑但没带 token）。
 */
export function probeBackend(baseUrl: string): Promise<BackendStatus> {
  const target = `${baseUrl}${PROBE_PATH}`;
  return new Promise((resolve) => {
    let settled = false;
    const finish = (online: boolean, message: string): void => {
      if (settled) {
        return;
      }
      settled = true;
      resolve({ online, baseUrl, checkedAt: Date.now(), message });
    };

    const request = net.request({ method: 'GET', url: target });
    const timer = setTimeout(() => {
      request.abort();
      finish(false, `连接超时（${PROBE_TIMEOUT_MS}ms）：${target}`);
    }, PROBE_TIMEOUT_MS);

    request.on('response', (response) => {
      clearTimeout(timer);
      // 挂一个空监听把响应体消费掉，避免连接悬挂
      response.on('data', () => {});
      finish(true, `HTTP ${response.statusCode}`);
    });
    request.on('error', (error) => {
      clearTimeout(timer);
      finish(false, `无法连接后端：${error.message}`);
    });
    request.end();
  });
}
