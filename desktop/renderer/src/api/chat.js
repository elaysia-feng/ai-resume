import { API_BASE_URL, apiClient, unwrapApiData } from './request.js';

// 创建会话：body {title?, resumeId?} -> {sessionId}
export async function createChatSession(data = {}) {
  const res = await apiClient.post('/api/chat/sessions', data);
  return unwrapApiData(res);
}

// 会话列表：[{id,title,lastMessageAt,messageCount}]，按最后消息时间倒序
export async function listChatSessions() {
  const res = await apiClient.get('/api/chat/sessions');
  return unwrapApiData(res) || [];
}

// 消息列表：[{id,role,content,contentType,toolName,seqNo,status,createdAt}]，按 seqNo 升序
export async function listChatMessages(sessionId) {
  const res = await apiClient.get(`/api/chat/sessions/${sessionId}/messages`);
  return unwrapApiData(res) || [];
}

// 删除会话（连带消息）
export async function deleteChatSession(sessionId) {
  const res = await apiClient.delete(`/api/chat/sessions/${sessionId}`);
  return unwrapApiData(res);
}

// 当前工作区根目录：{path}（没设过时 path 为 null）
export async function getChatWorkspace() {
  const res = await apiClient.get('/api/chat/workspace');
  return unwrapApiData(res);
}

// 设置工作区根目录：body {path} -> {path}
export async function setChatWorkspace(path) {
  const res = await apiClient.post('/api/chat/workspace', { path });
  return unwrapApiData(res);
}

/**
 * 发消息并消费 SSE 流。
 * 事件名与数据格式见 docs/desktop-contract.md 第 3.2 节：
 * text_delta / tool_call / tool_result / done / error
 *
 * @param {number|string} sessionId 会话 ID
 * @param {{content: string, workspacePath?: string}} payload 请求体
 * @param {{onTextDelta?: Function, onToolCall?: Function, onToolResult?: Function,
 *          onDone?: Function, onError?: Function, onEvent?: Function}} handlers 事件回调
 * @param {{signal?: AbortSignal}} options 中断信号（停止生成）
 */
export async function streamChatMessage(sessionId, payload = {}, handlers = {}, options = {}) {
  const token = localStorage.getItem('jwt_token');
  const requestBody = { content: payload.content ?? '' };
  if (payload.workspacePath) {
    requestBody.workspacePath = payload.workspacePath;
  }

  let response;
  try {
    response = await fetch(`${API_BASE_URL}/api/chat/sessions/${sessionId}/messages`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify(requestBody),
      signal: options.signal,
    });
  } catch (error) {
    // 中断是用户主动行为，原样抛出由调用方区分处理
    if (error?.name === 'AbortError') {
      throw error;
    }
    throw new Error('无法连接后端服务，请确认后端已启动');
  }

  if (!response.ok) {
    throw new Error(await readResponseError(response));
  }
  if (!response.body) {
    throw new Error('当前浏览器不支持流式响应');
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';

  while (true) {
    const { value, done } = await reader.read();
    if (done) {
      break;
    }
    buffer += decoder.decode(value, { stream: true });
    buffer = normalizeLineBreaks(buffer);
    // 空行分隔一条事件，最后一段可能是半条，留到下一轮
    const chunks = buffer.split('\n\n');
    buffer = chunks.pop() || '';
    for (const chunk of chunks) {
      emitSseEvent(chunk, handlers);
    }
  }

  buffer += decoder.decode();
  if (buffer.trim()) {
    emitSseEvent(buffer, handlers);
  }
}

// 统一换行，避免 \r\n 让空行分隔判断失效
function normalizeLineBreaks(text) {
  return text.replace(/\r\n/g, '\n').replace(/\r/g, '\n');
}

// 解析一条 SSE 事件：event 行给事件名，data 行（可多行）拼成 JSON
function emitSseEvent(chunk, handlers) {
  let eventName = 'message';
  const dataLines = [];

  for (const rawLine of chunk.split('\n')) {
    const line = rawLine.replace(/\r$/, '');
    // 空行与注释行（以冒号开头）直接忽略
    if (!line || line.startsWith(':')) {
      continue;
    }

    const colonIndex = line.indexOf(':');
    const field = colonIndex === -1 ? line : line.slice(0, colonIndex);
    let value = colonIndex === -1 ? '' : line.slice(colonIndex + 1);
    if (value.startsWith(' ')) {
      value = value.slice(1);
    }

    if (field === 'event') {
      eventName = value;
    } else if (field === 'data') {
      dataLines.push(value);
    }
  }

  if (!dataLines.length) {
    return;
  }

  const rawData = dataLines.join('\n');
  if (!rawData.trim()) {
    return;
  }

  let data;
  try {
    data = JSON.parse(rawData);
  } catch {
    data = { text: rawData };
  }
  // 兼容后端把 SSE 数据也包一层 ApiResponse 的情况
  if (data && typeof data === 'object' && 'code' in data && 'data' in data) {
    data = data.data ?? data;
  }

  handlers.onEvent?.(eventName, data);

  if (eventName === 'text_delta') {
    handlers.onTextDelta?.(data);
  } else if (eventName === 'tool_call') {
    handlers.onToolCall?.(data);
  } else if (eventName === 'tool_result') {
    handlers.onToolResult?.(data);
  } else if (eventName === 'done') {
    handlers.onDone?.(data);
  } else if (eventName === 'error') {
    handlers.onError?.(data);
  }
}

// 失败响应体可能是 {code,message,data}，也可能是一段纯文本
async function readResponseError(response) {
  const fallback = `对话请求失败（HTTP ${response.status}）`;
  try {
    const text = await response.text();
    if (!text) {
      return fallback;
    }
    try {
      const parsed = JSON.parse(text);
      return parsed?.message || parsed?.detail || fallback;
    } catch {
      return text.slice(0, 200) || fallback;
    }
  } catch {
    return fallback;
  }
}
