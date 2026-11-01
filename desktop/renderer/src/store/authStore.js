import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { getCurrentUser } from '../api/auth.js';
import { createLocalProfile, DEFAULT_LOCAL_DISPLAY_NAME } from '../api/localProfile.js';

const TOKEN_STORAGE_KEY = 'jwt_token';

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_STORAGE_KEY) || null);
  const localSessionState = ref(token.value ? 'ready' : 'idle');
  const hasLocalSession = computed(() => Boolean(token.value));
  let initializationPromise = null;
  let localSessionPromise = null;
  let initialized = false;

  async function initialize() {
    if (initialized) {
      return;
    }
    if (initializationPromise) {
      return initializationPromise;
    }

    initializationPromise = (async () => {
      if (token.value) {
        try {
          await getCurrentUser();
          localSessionState.value = 'ready';
        } catch {
          clearLocalToken();
        }
      }
      initialized = true;
    })().finally(() => {
      initializationPromise = null;
    });
    return initializationPromise;
  }

  async function ensureLocalSession() {
    await initialize();
    if (token.value) {
      return true;
    }
    if (localSessionPromise) {
      return localSessionPromise;
    }

    localSessionState.value = 'loading';
    localSessionPromise = createLocalProfile(DEFAULT_LOCAL_DISPLAY_NAME)
      .then((data) => {
        const nextToken = data?.token || data?.access_token || data?.jwt;
        if (!nextToken) {
          throw new Error('本地会话响应缺少令牌');
        }
        token.value = nextToken;
        localStorage.setItem(TOKEN_STORAGE_KEY, nextToken);
        localSessionState.value = 'ready';
        return true;
      })
      .catch((error) => {
        localSessionState.value = 'error';
        throw error;
      })
      .finally(() => {
        localSessionPromise = null;
      });
    return localSessionPromise;
  }

  function clearLocalToken() {
    token.value = null;
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    localSessionState.value = 'idle';
  }

  return {
    token,
    localSessionState,
    hasLocalSession,
    ensureLocalSession,
    initialize,
  };
});
