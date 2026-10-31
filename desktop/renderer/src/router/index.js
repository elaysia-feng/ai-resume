import { createRouter, createWebHashHistory, createWebHistory } from 'vue-router';
import { useAuthStore } from '../store/authStore.js';
import { createResume } from '../api/resumes.js';

const routes = [
  {
    path: '/',
    name: 'Lobby',
    component: () => import('../views/LobbyView.vue'),
    meta: { guest: true },
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginView.vue'),
    meta: { guest: true },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/RegisterView.vue'),
    meta: { guest: true },
  },
  {
    path: '/welcome',
    redirect: { name: 'Home' },
  },
  {
    path: '/dashboard',
    component: () => import('../views/DashboardLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('../views/HomeView.vue'),
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/ProfileView.vue'),
      },
      {
        path: 'history',
        name: 'History',
        component: () => import('../views/HistoryView.vue'),
      },
      {
        path: 'interview',
        name: 'Interview',
        component: () => import('../views/InterviewView.vue'),
      },
      {
        path: 'templates',
        name: 'Templates',
        component: () => import('../views/TemplatesView.vue'),
      },
    ],
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('../views/ChatView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('../views/SettingsView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/editor/:id',
    name: 'ResumeEditor',
    component: () => import('../views/ResumeEditorView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/resume/create',
    name: 'ResumeCreate',
    meta: { requiresAuth: true },
    beforeEnter: async (to) => {
      try {
        const created = await createResume({ title: '新建简历' });
        const resumeId = created?.resumeId ?? created?.id ?? created?._id;
        if (resumeId) {
          return { name: 'ResumeEditor', params: { id: resumeId }, query: to.query };
        }
      } catch {
        return { name: 'Home', query: { resumeCreateError: 'failed' } };
      }
      return { name: 'Home', query: { resumeCreateError: 'missing-id' } };
    },
  },
];

const router = createRouter({
  // 桌面端用 file:// 加载渲染层时，location.pathname 是磁盘路径，history 模式匹配不到路由，
  // 所以相对 base（BASE_URL === './'，桌面端构建）改用 hash 模式；网页端仍是 history 模式。
  history: import.meta.env.BASE_URL === './' ? createWebHashHistory() : createWebHistory(),
  routes,
});

// 桌面端默认进入游客工作台；网页端保持原来的登录 / 注册流程。
const isDesktopApp = typeof window !== 'undefined' && !!window.desktop;

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore();

  if (isDesktopApp) {
    if (to.name === 'Lobby' || to.name === 'Welcome') {
      next({ name: 'Home' });
      return;
    }
    next();
    return;
  }

  const requiresAuth = to.meta.requiresAuth;
  const isGuest = to.meta.guest;

  if (requiresAuth && !authStore.isAuthenticated) {
    next({ name: 'Login', query: { redirect: to.fullPath } });
    return;
  }

  if (isGuest && authStore.isAuthenticated) {
    next({ name: 'Home' });
    return;
  }

  next();
});

export default router;
