import { apiClient, unwrapApiData } from './request.js';

/** 发送验证码 */
export async function sendCode(email, type = 'register') {
  const res = await apiClient.post('/api/auth/send-code', { email, type });
  return unwrapApiData(res);
}

/** 验证验证码，返回 verifyToken */
export async function verifyCode(email, code) {
  const res = await apiClient.post('/api/auth/verify-code', { email, code });
  return unwrapApiData(res);
}

/** 用 verifyToken 设置密码完成注册 */
export async function setPassword(verifyToken, username, password) {
  const res = await apiClient.post('/api/auth/set-password', { verifyToken, username, password });
  return unwrapApiData(res);
}

/** 邮箱+验证码登录 */
export async function loginByCode(email, code) {
  const res = await apiClient.post('/api/auth/login-by-code', { email, code });
  return unwrapApiData(res);
}

/** 登录（支持 username 或 email） */
export async function login(credentials) {
  const res = await apiClient.post('/api/auth/login', credentials);
  return unwrapApiData(res);
}

/** 获取当前登录用户信息 */
export async function getCurrentUser() {
  const res = await apiClient.get('/api/auth/me');
  return unwrapApiData(res);
}

/** 上传头像 */
export async function uploadAvatar(file) {
  const formData = new FormData();
  formData.append('file', file);

  const res = await apiClient.post('/api/auth/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return unwrapApiData(res);
}

/** 注册（直接注册） */
export async function register(data) {
  const res = await apiClient.post('/api/auth/register', data);
  return unwrapApiData(res);
}

export function logout() {
  localStorage.removeItem('jwt_token');
}
