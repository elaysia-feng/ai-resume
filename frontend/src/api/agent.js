import { API_BASE_URL, apiClient, unwrapApiData } from './request.js';

const MOCK_DELAY_MS = 450;

export function shouldUseAgentMock() {
  if (typeof window === 'undefined') {
    return false;
  }
  const storedValue = window.localStorage.getItem('agent_copilot_mock');
  if (storedValue === 'false') {
    return false;
  }
  if (storedValue === 'true') {
    return true;
  }
  return import.meta.env.VITE_AGENT_MOCK !== 'false';
}

export function setAgentMockEnabled(enabled) {
  if (typeof window !== 'undefined') {
    window.localStorage.setItem('agent_copilot_mock', enabled ? 'true' : 'false');
  }
}

export async function streamAgentRun(sessionId, payload, handlers = {}, options = {}) {
  if (options.mock ?? shouldUseAgentMock()) {
    return streamMockAgentRun(sessionId, payload, handlers);
  }

  const queuedRun = await createAgentRun(sessionId, payload);
  handlers.onQueued?.(queuedRun);
  await streamAgentRunEvents(queuedRun.runId, handlers);
  return queuedRun;
}

export async function continueAgentRun(runId, payload, handlers = {}, options = {}) {
  if (options.mock ?? shouldUseAgentMock()) {
    return streamMockContinueRun(runId, payload, handlers);
  }

  const queuedRun = await enqueueContinueAgentRun(runId, payload);
  handlers.onQueued?.(queuedRun);
  await streamAgentRunEvents(queuedRun.runId, handlers);
  return queuedRun;
}

export async function createAgentRun(sessionId, payload) {
  const res = await apiClient.post(`/api/agent/sessions/${sessionId}/runs`, payload);
  return unwrapApiData(res);
}

export async function enqueueContinueAgentRun(runId, payload) {
  const res = await apiClient.post(`/api/agent/runs/${runId}/continue`, payload);
  return unwrapApiData(res);
}

export async function streamAgentRunEvents(runId, handlers = {}, options = {}) {
  const afterSeq = options.afterSeq || 0;
  return streamSseGet(`/api/agent/runs/${runId}/events/stream?afterSeq=${encodeURIComponent(afterSeq)}`, handlers);
}

export async function approveAgentRun(runId, payload, options = {}) {
  if (options.mock ?? shouldUseAgentMock()) {
    return {
      runId,
      status: 'SUCCESS',
      resumeId: payload?.resumeId,
      appliedPatchCount: payload?.approvedPatchIds?.length || 1,
    };
  }

  const res = await apiClient.post(`/api/agent/runs/${runId}/approve`, payload);
  return unwrapApiData(res);
}

export async function cancelAgentRun(runId, payload = {}) {
  const res = await apiClient.post(`/api/agent/runs/${runId}/cancel`, payload);
  return unwrapApiData(res);
}

export async function getAgentRun(runId) {
  const res = await apiClient.get(`/api/agent/runs/${runId}`);
  return unwrapApiData(res);
}

export async function getAgentRunEvents(runId, params = {}) {
  const res = await apiClient.get(`/api/agent/runs/${runId}/events`, { params });
  return unwrapApiData(res) || [];
}

async function streamSse(path, payload, handlers) {
  const token = localStorage.getItem('jwt_token');
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok || !response.body) {
    throw new Error(`Agent stream failed: ${response.status}`);
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';

  while (true) {
    const { value, done } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    const chunks = buffer.split('\n\n');
    buffer = chunks.pop() || '';
    chunks.forEach((chunk) => emitSseChunk(chunk, handlers));
  }

  if (buffer.trim()) {
    emitSseChunk(buffer, handlers);
  }
}

async function streamSseGet(path, handlers) {
  const token = localStorage.getItem('jwt_token');
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'GET',
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });

  if (!response.ok || !response.body) {
    throw new Error(`Agent event stream failed: ${response.status}`);
  }

  await readSseBody(response.body, handlers);
}

async function readSseBody(body, handlers) {
  const reader = body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';

  while (true) {
    const { value, done } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    const chunks = buffer.split('\n\n');
    buffer = chunks.pop() || '';
    chunks.forEach((chunk) => emitSseChunk(chunk, handlers));
  }

  if (buffer.trim()) {
    emitSseChunk(buffer, handlers);
  }
}

function emitSseChunk(chunk, handlers) {
  const dataLine = chunk.split('\n').find((line) => line.startsWith('data:'));
  if (!dataLine) return;
  const rawData = dataLine.slice(5).trim();
  if (!rawData) return;
  const event = JSON.parse(rawData);
  handlers.onEvent?.(event);
  if (event.eventType === 'clarification.required') {
    handlers.onClarification?.(event.payload || {});
  }
  if (event.eventType === 'approval.required') {
    handlers.onApproval?.(event.payload || {});
  }
}

async function streamMockAgentRun(sessionId, payload, handlers) {
  const runId = Date.now();
  const events = [
    buildEvent(runId, sessionId, 1, 'run.started', 'BOOTSTRAP', 'Agent run 已启动'),
    buildEvent(runId, sessionId, 2, 'stage.changed', 'JD_ANALYST', '分析岗位 JD'),
    buildEvent(runId, sessionId, 3, 'stage.changed', 'REWRITER', '生成当前模块修改提案'),
    buildEvent(runId, sessionId, 4, 'stage.changed', 'REVIEWER', '审查修改提案'),
    buildEvent(runId, sessionId, 5, 'approval.required', 'APPROVAL_PACKAGER', '已生成待确认修改建议', {
      runId,
      resumeId: payload.resumeId,
      summary: 'Mock 已生成当前模块的整块替换建议',
      riskNotes: ['Mock 数据仅用于前端联调'],
      patches: [
        {
          patchId: `patch-${payload.targetSectionId}-1`,
          sectionId: payload.targetSectionId,
          sectionTitle: payload.sectionTitle,
          operation: 'REPLACE_SECTION_CONTENT',
          reason: '根据输入任务生成当前模块建议',
          beforeJson: payload.beforeJson || {},
          afterJson: payload.beforeJson || {},
          changeSummary: '当前 mock 未改写内容，只验证确认流程',
          riskLevel: 'LOW',
        },
      ],
    }),
  ];

  for (const event of events) {
    await delay(MOCK_DELAY_MS);
    handlers.onEvent?.(event);
    if (event.eventType === 'approval.required') {
      handlers.onApproval?.(event.payload);
    }
  }
}

async function streamMockContinueRun(runId, payload, handlers) {
  await delay(MOCK_DELAY_MS);
  handlers.onEvent?.(buildEvent(runId, payload?.sessionId, 1, 'stage.changed', 'SUPERVISOR', '已收到补充信息'));
}

function buildEvent(runId, sessionId, eventSeq, eventType, stageCode, message, payload = {}) {
  return { runId, sessionId, eventSeq, eventType, stageCode, message, payload };
}

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
