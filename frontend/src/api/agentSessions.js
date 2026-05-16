import { apiClient, unwrapApiData } from './request.js';

export async function createAgentSession(data) {
  const res = await apiClient.post('/api/agent/sessions', data);
  return unwrapApiData(res);
}

export async function listAgentSessions(params = {}) {
  const res = await apiClient.get('/api/agent/sessions', { params });
  return unwrapApiData(res) || [];
}

export async function getAgentSessionDetail(sessionId) {
  const res = await apiClient.get(`/api/agent/sessions/${sessionId}`);
  return unwrapApiData(res);
}

export async function updateAgentSession(sessionId, data) {
  const res = await apiClient.put(`/api/agent/sessions/${sessionId}`, data);
  return unwrapApiData(res);
}

export async function deleteAgentSession(sessionId) {
  const res = await apiClient.delete(`/api/agent/sessions/${sessionId}`);
  return unwrapApiData(res);
}

export async function listAgentMessages(sessionId) {
  const res = await apiClient.get(`/api/agent/sessions/${sessionId}/messages`);
  return unwrapApiData(res) || [];
}

export async function createAgentMessage(sessionId, data) {
  const res = await apiClient.post(`/api/agent/sessions/${sessionId}/messages`, data);
  return unwrapApiData(res);
}
