import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import router from './router/index.js';
import { useAuthStore } from './store/authStore.js';
import './styles/main.css';
import './styles/green-theme.css';

const pinia = createPinia();
const app = createApp(App);

app.use(pinia);
useAuthStore(pinia).initialize().catch(() => {});
app.use(router);
app.mount('#app');
