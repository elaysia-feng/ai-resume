import { apiClient, unwrapApiData } from './request.js';

/**
 * 本地工作区在后端固定使用的身份信息。
 *
 * <p>后端用固定邮箱复用本机数据归属记录。它不是用户账号，界面不提供登录流程。</p>
 */
export const DEFAULT_LOCAL_DISPLAY_NAME = '本地用户';

/**
 * 创建或复用本机数据归属记录，并取得本地 API 所需的请求令牌。
 *
 * <p>幂等：重复调用会返回同一个本机身份，不会新建用户。</p>
 *
 * @param {string} displayName 本机记录的显示名，不是简历里的姓名
 */
export async function createLocalProfile(displayName = DEFAULT_LOCAL_DISPLAY_NAME) {
  const res = await apiClient.post('/api/auth/local-profile', { displayName }, { timeout: 15000 });
  return unwrapApiData(res);
}
