import { createRouter, createWebHistory } from 'vue-router'

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
    path: '/play',
    name: 'ParticlePlay',
    component: () => import('../views/ParticlePlay.vue'),
    meta: { adminOnly: true }
  },
  {
    path: '/image/:id',
    name: 'ImageDetail',
    component: () => import('../views/ImageDetail.vue')
  },
  {
    path: '/profile/:id',
    name: 'Profile',
    component: () => import('../views/Profile.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('satoken')
  const hostname = window.location.hostname
  const isAdminDomain = hostname === 'admin.image-space.app' || hostname === 'localhost'

  if (to.meta.adminOnly && !isAdminDomain) {
    next('/home')
  } else if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if ((to.path === '/login' || to.path === '/register') && token) {
    next('/home')
  } else {
    next()
  }
})

export default router
