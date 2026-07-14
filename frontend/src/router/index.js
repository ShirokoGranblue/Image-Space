import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '../utils/token'
import { isAllowedAdminDomain } from '../utils/adminDomain'
import { useUserStore } from '../store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue')
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('../views/Forbidden.vue')
  },
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/square',
    name: 'ImageSquare',
    component: () => import('../views/ImageSquare.vue')
  },
  {
    path: '/image/:uuid',
    name: 'ImageDetail',
    component: () => import('../views/ImageDetail.vue')
  },
  {
    path: '/profile/:uuid',
    name: 'Profile',
    component: () => import('../views/Profile.vue')
  },
  {
    path: '/admin/audit-log',
    name: 'AuditLogMonitor',
    component: () => import('../views/admin/AuditLogMonitor.vue'),
    meta: {
      title: '审计日志监控',
      requiresAuth: true,
      requiresAdmin: true,
      requiresAdminDomain: true
    }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/NotFound.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const token = getToken()

  if (to.meta.requiresAdminDomain && !isAllowedAdminDomain()) {
    next('/403')
    return
  }

  if (to.meta.requiresAuth && !token) {
    next('/login')
    return
  }

  if ((to.path === '/login' || to.path === '/register') && token) {
    next('/home')
    return
  }

  if (to.meta.requiresAdmin) {
    const userStore = useUserStore()
    if (!userStore.userInfo) {
      await userStore.fetchUserInfo()
    }
    if (userStore.userInfo?.role !== 'admin') {
      next('/403')
      return
    }
  }

  next()
})

export default router
