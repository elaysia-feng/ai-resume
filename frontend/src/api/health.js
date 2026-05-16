import { apiClient } from './request.js';

export async function getJavaHealth() {
  const res = await apiClient.get('/api/health');
  return res.data;
}
