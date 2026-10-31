import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import router from './router/index.js';
import { useAuthStore } from './store/authStore.js';
import './styles/design-system.css';
import './styles/main.css';
import './styles/green-theme.css';

const themeQuery = window.matchMedia('(prefers-color-scheme: dark)');
const themeStorageKey = 'resume_forge_theme';

function applyTheme(preferredTheme, prefersDark = themeQuery.matches) {
  document.documentElement.dataset.theme = ['light', 'dark'].includes(preferredTheme)
    ? preferredTheme
    : prefersDark ? 'dark' : 'light';
}

applyTheme(localStorage.getItem(themeStorageKey));
themeQuery.addEventListener('change', (event) => {
  const preferredTheme = localStorage.getItem(themeStorageKey);
  if (!['light', 'dark'].includes(preferredTheme)) {
    applyTheme(preferredTheme, event.matches);
  }
});

const pinia = createPinia();
const app = createApp(App);

app.use(pinia);
const authStore = useAuthStore(pinia);
authStore.initialize().catch(() => {}).finally(() => {
  if (window.desktop && !authStore.isAuthenticated) {
    authStore.loginAsGuest().catch(() => {});
  }
});
app.use(router);
app.mount('#app');
