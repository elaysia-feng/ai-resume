import { apiClient, unwrapApiData } from './request.js';

export async function startInterviewRun(data) {
  const res = await apiClient.post('/api/interview/runs', data);
  return unwrapApiData(res);
}

export async function getInterviewBoard(runId) {
  const res = await apiClient.get(`/api/interview/runs/${runId}/board`);
  return unwrapApiData(res);
}

export async function submitInterviewAnswer(roundId, data) {
  const res = await apiClient.post(`/api/interview/interquestion-rounds/${roundId}/answer`, data);
  return unwrapApiData(res);
}

export async function getInterviewQuestionRounds(runId, params = {}) {
  const res = await apiClient.get(`/api/interview/runs/${runId}/question-rounds`, { params });
  return unwrapApiData(res) || { records: [], total: 0, pageNum: 1, pageSize: 10 };
}

export async function finishInterviewRun(runId) {
  const res = await apiClient.post(`/api/interview/runs/${runId}/finish`);
  return unwrapApiData(res);
}
