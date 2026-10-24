import axios from 'axios';

export const API_BASE_URL = 'http://localhost:8080';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 120000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Attach JWT from localStorage on every request
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('jwt_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Handle 401/403 by clearing invalid token
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      localStorage.removeItem('jwt_token');
      localStorage.removeItem('resume_forge_user');
    }
    return Promise.reject(error);
  }
);

export function unwrapApiData(response) {
  const payload = response?.data;
  if (payload && typeof payload === 'object' && 'code' in payload && 'message' in payload) {
    if (payload.code !== 200) {
      throw new Error(payload.message || '请求失败');
    }
    return payload.data;
  }
  return payload;
}

export function extractApiMessage(error, fallback = '请求失败') {
  const responseData = error?.response?.data;
  if (responseData?.message) {
    return responseData.message;
  }
  if (responseData?.detail) {
    if (typeof responseData.detail === 'string') {
      return responseData.detail;
    }
    if (Array.isArray(responseData.detail)) {
      return responseData.detail.map((item) => item?.msg || item?.message || String(item)).join('；');
    }
  }
  return error?.message || fallback;
}

// 本地磁盘文件（头像等）存在后端机器上，数据库里存的是相对路径，形如 avatar/xxx.png
const LOCAL_FILE_PATTERN = /^(avatar|resume-avatar)\/[^/]+$/;

export function resolveApiAssetUrl(value) {
  if (typeof value !== 'string') {
    return '';
  }

  const trimmedValue = value.trim();
  if (!trimmedValue) {
    return '';
  }

  if (
    trimmedValue.startsWith('data:')
    || trimmedValue.startsWith('blob:')
    || /^[a-z][a-z\d+\-.]*:\/\//i.test(trimmedValue)
    || trimmedValue.startsWith('//')
  ) {
    return trimmedValue;
  }

  // 本地存储的相对路径 -> 带鉴权参数的文件下载接口（<img> 标签带不了请求头）
  if (LOCAL_FILE_PATTERN.test(trimmedValue)) {
    const token = localStorage.getItem('jwt_token');
    const fileUrl = `${API_BASE_URL}/api/files/${trimmedValue}`;
    return token ? `${fileUrl}?token=${encodeURIComponent(token)}` : fileUrl;
  }

  const normalizedPath = trimmedValue.startsWith('/') ? trimmedValue : `/${trimmedValue}`;
  return new URL(normalizedPath, API_BASE_URL).toString();
}
