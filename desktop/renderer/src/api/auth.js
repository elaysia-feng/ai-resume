import { apiClient, unwrapApiData } from './request.js';

/** 读取本地 API 会话对应的用户记录，用于检查保存的令牌是否仍有效。 */
export async function getCurrentUser() {
  const res = await apiClient.get('/api/auth/me');
  return unwrapApiData(res);
}
