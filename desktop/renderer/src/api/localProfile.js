import { apiClient, unwrapApiData } from './request.js';

/**
 * 本地身份的固定占位邮箱。
 *
 * <p>后端用它唯一标识本机的本地账号（desktop 端首次启动创建的那一个）。
 * 它是保留域名，永远收不到邮件，所以本地身份不存在密码 / 验证码登录。</p>
 */
export const LOCAL_IDENTITY_EMAIL = 'local@local.invalid';

/**
 * 创建（或复用）本地身份。
 *
 * <p>幂等：调多少次都只会是本机那一个本地账号，不会新建用户。
 * 返回结构和现有注册接口一致：token / type / userId / username / email。</p>
 *
 * @param {string} displayName 昵称（账号身份，不是简历里的姓名）
 */
export async function createLocalProfile(displayName) {
  const res = await apiClient.post('/api/auth/local-profile', { displayName });
  return unwrapApiData(res);
}

/**
 * 当前登录的用户是不是本地身份。
 *
 * @param {{ email?: string } | null | undefined} user
 */
export function isLocalIdentity(user) {
  return !!user?.email && String(user.email).toLowerCase() === LOCAL_IDENTITY_EMAIL;
}
