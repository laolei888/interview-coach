import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: { title: '训练驾驶舱' }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/resume',
    name: 'ResumeAnalysis',
    component: () => import('../views/ResumeAnalysis.vue'),
    meta: { requiresAuth: true, title: '简历分析' }
  },
  {
    path: '/interview',
    name: 'MockInterview',
    component: () => import('../views/MockInterview.vue'),
    meta: { requiresAuth: true, title: 'AI 模拟面试' }
  },
  {
    path: '/history',
    name: 'History',
    component: () => import('../views/History.vue'),
    meta: { requiresAuth: true, title: '训练记录' }
  },
  {
    path: '/questions',
    name: 'QuestionBank',
    component: () => import('../views/QuestionBank.vue'),
    meta: { requiresAuth: true, title: '面试题库' }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue'),
    meta: { requiresAuth: true, title: '个人中心' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/NotFound.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.hash) return { el: to.hash, behavior: 'smooth' }
    return { top: 0, left: 0, behavior: 'smooth' }
  }
})

// 路由守卫：未登录跳转登录页
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

router.afterEach((to) => {
  document.title = `${to.meta.title || '求职训练'} - Interview Coach`
})

export default router
