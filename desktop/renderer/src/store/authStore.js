import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import {
  login as apiLogin,
  register as apiRegister,
  logout as apiLogout,
  loginByCode as apiLoginByCode,
  getCurrentUser as apiGetCurrentUser,
  uploadAvatar as apiUploadAvatar,
} from '../api/auth.js';
import { createLocalProfile, DEFAULT_GUEST_DISPLAY_NAME } from '../api/localProfile.js';
import { resolveApiAssetUrl } from '../api/request.js';

const USER_STORAGE_KEY = 'resume_forge_user';

function readStoredUser() {
  const raw = localStorage.getItem(USER_STORAGE_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw);
  } catch {
    localStorage.removeItem(USER_STORAGE_KEY);
    return null;
  }
}

function decodeJwtPayload(token) {
  if (!token) {
    return {};
  }

  try {
    const payload = token.split('.')[1];
    if (!payload) {
      return {};
    }

    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const decoded = decodeURIComponent(
      atob(base64)
        .split('')
        .map((char) => `%${char.charCodeAt(0).toString(16).padStart(2, '0')}`)
        .join('')
    );
    return JSON.parse(decoded);
  } catch {
    return {};
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('jwt_token') || null);
  const user = ref(readStoredUser());
  const guestSessionState = ref('idle');
  let guestSessionPromise = null;

  const isAuthenticated = computed(() => !!token.value);
  const currentUser = computed(() => user.value);

  function normalizeUser(data = {}, currentToken = token.value) {
    const payload = decodeJwtPayload(currentToken);
    const userId = data.userId ?? data.id ?? payload.userId ?? null;
    const username = data.username ?? data.name ?? payload.sub ?? '';
    const email = data.email ?? '';
    const avatarUrl = resolveApiAssetUrl(data.avatarUrl || data.avatar || '');

    if (!userId && !username && !email) {
      return null;
    }

    return {
      userId,
      username,
      email,
      avatarUrl,
      enabled: data.enabled ?? true,
      createdAt: data.createdAt || '',
      updatedAt: data.updatedAt || '',
    };
  }

  function persistUser(nextUser) {
    user.value = nextUser;
    if (nextUser) {
      localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(nextUser));
      return;
    }
    localStorage.removeItem(USER_STORAGE_KEY);
  }

  function _storeToken(data) {
    if (!data) {
      throw new Error('登录响应为空');
    }
    const t = data.token || data.access_token || data.jwt;
    if (!t) {
      throw new Error('登录响应缺少 token');
    }

    token.value = t;
    localStorage.setItem('jwt_token', t);

    const nextUser = normalizeUser(data, t);
    if (nextUser) {
      persistUser(nextUser);
    }
  }

  async function login(credentials) {
    const data = await apiLogin(credentials);
    _storeToken(data);
  }

  async function loginByCode(email, code) {
    const data = await apiLoginByCode(email, code);
    _storeToken(data);
  }

  function storeAuthResponse(data) {
    _storeToken(data);
  }

  async function loginAsGuest() {
    if (token.value) {
      return user.value;
    }
    if (guestSessionPromise) {
      return guestSessionPromise;
    }

    guestSessionState.value = 'loading';
    guestSessionPromise = createLocalProfile(DEFAULT_GUEST_DISPLAY_NAME)
      .then((data) => {
        if (!token.value) {
          _storeToken(data);
        }
        guestSessionState.value = 'ready';
        return user.value;
      })
      .catch((error) => {
        guestSessionState.value = 'error';
        throw error;
      })
      .finally(() => {
        guestSessionPromise = null;
      });
    return guestSessionPromise;
  }

  async function register(data) {
    const resp = await apiRegister(data);
    _storeToken(resp.user || resp);
  }

  async function refreshCurrentUser() {
    if (!token.value) {
      persistUser(null);
      guestSessionState.value = 'idle';
      return null;
    }

    const data = await apiGetCurrentUser();
    const nextUser = normalizeUser(data, token.value);
    if (nextUser) {
      persistUser(nextUser);
    }
    return nextUser;
  }

  async function uploadAvatar(file) {
    const data = await apiUploadAvatar(file);
    const nextUser = normalizeUser(data, token.value);
    if (nextUser) {
      persistUser(nextUser);
    }
    return nextUser;
  }

  async function initialize() {
    if (!token.value) {
      persistUser(null);
      return;
    }

    const localUser = normalizeUser(user.value || {}, token.value);
    if (localUser) {
      persistUser(localUser);
    }

    try {
      await refreshCurrentUser();
    } catch (error) {
      const fallbackUser = normalizeUser(user.value || {}, token.value);
      if (fallbackUser) {
        persistUser(fallbackUser);
        return;
      }
      logout();
      throw error;
    }
  }

  function logout() {
    token.value = null;
    persistUser(null);
    guestSessionState.value = 'idle';
    apiLogout();
  }

  return {
    token,
    user,
    guestSessionState,
    isAuthenticated,
    currentUser,
    login,
    loginByCode,
    storeAuthResponse,
    loginAsGuest,
    register,
    refreshCurrentUser,
    uploadAvatar,
    initialize,
    logout,
  };
});
