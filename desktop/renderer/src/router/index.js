import { createRouter, createWebHashHistory, createWebHistory } from 'vue-router';
import { createResume } from '../api/resumes.js';
import { useAuthStore } from '../store/authStore.js';

const routes = [
  {
    path: '/',
    redirect: { name: 'Home' },
  },
  {
    path: '/dashboard',
    component: () => import('../views/DashboardLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('../views/HomeView.vue'),
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
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('../views/SettingsView.vue'),
  },
  {
    path: '/editor/:id',
    name: 'ResumeEditor',
    component: () => import('../views/ResumeEditorView.vue'),
  },
  {
    path: '/resume/create',
    name: 'ResumeCreate',
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
  {
    path: '/:pathMatch(.*)*',
    redirect: { name: 'Home' },
  },
];

const router = createRouter({
  // 桌面端用 file:// 加载渲染层时，location.pathname 是磁盘路径，history 模式匹配不到路由，
  // 所以相对 base（BASE_URL === './'，桌面端构建）改用 hash 模式；网页端仍是 history 模式。
  history: import.meta.env.BASE_URL === './' ? createWebHashHistory() : createWebHistory(),
  routes,
});

router.beforeEach(async (to) => {
  if (to.name === 'Home') {
    return true;
  }

  try {
    await useAuthStore().ensureLocalSession();
    return true;
  } catch {
    return { name: 'Home' };
  }
});

export default router;
