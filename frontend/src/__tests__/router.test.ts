import { describe, it, expect, beforeEach, vi } from 'vitest'
import { createRouter, createWebHistory, type RouteLocationNormalized } from 'vue-router'

// Build a test router with the same structure as the real one
function createTestRouter(initialPath = '/') {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/login', name: 'Login', component: { template: '<div>Login</div>' } },
      { path: '/register', name: 'Register', component: { template: '<div>Register</div>' } },
      { path: '/', redirect: '/home' },
      { path: '/home', name: 'Home', component: { template: '<div>Home</div>' }, meta: { requiresAuth: true } },
      { path: '/square', name: 'ImageSquare', component: { template: '<div>Square</div>' } },
      { path: '/image/:id', name: 'ImageDetail', component: { template: '<div>Detail</div>' } },
      { path: '/profile/:id', name: 'Profile', component: { template: '<div>Profile</div>' } },
    ],
  })
}

// Mirror the guard logic to test in isolation
function guardLogic(to: RouteLocationNormalized): string | null {
  const token = sessionStorage.getItem('satoken')
  if (to.meta.requiresAuth && !token) return '/login'
  if ((to.path === '/login' || to.path === '/register') && token) return '/home'
  return null // allow
}

describe('Router guard logic', () => {
  beforeEach(() => {
    sessionStorage.clear()
  })

  describe('Auth-required routes', () => {
    it('redirects to /login when no token', async () => {
      const router = createTestRouter()

      router.beforeEach((to, _from, next) => {
        const result = guardLogic(to)
        result ? next(result) : next()
      })

      await router.push('/home')
      await router.isReady()

      expect(router.currentRoute.value.path).toBe('/login')
    })

    it('allows navigation when token exists', async () => {
      sessionStorage.setItem('satoken', 'valid-token')
      const router = createTestRouter()

      router.beforeEach((to, _from, next) => {
        const result = guardLogic(to)
        result ? next(result) : next()
      })

      await router.push('/home')
      await router.isReady()

      expect(router.currentRoute.value.path).toBe('/home')
    })
  })

  describe('Public routes with existing token', () => {
    it('redirects /login to /home when already logged in', async () => {
      sessionStorage.setItem('satoken', 'valid-token')
      const router = createTestRouter()

      router.beforeEach((to, _from, next) => {
        const result = guardLogic(to)
        result ? next(result) : next()
      })

      await router.push('/login')
      await router.isReady()

      expect(router.currentRoute.value.path).toBe('/home')
    })

    it('redirects /register to /home when already logged in', async () => {
      sessionStorage.setItem('satoken', 'valid-token')
      const router = createTestRouter()

      router.beforeEach((to, _from, next) => {
        const result = guardLogic(to)
        result ? next(result) : next()
      })

      await router.push('/register')
      await router.isReady()

      expect(router.currentRoute.value.path).toBe('/home')
    })
  })

  describe('Public routes without token', () => {
    it('allows /login when no token', async () => {
      const router = createTestRouter()

      router.beforeEach((to, _from, next) => {
        const result = guardLogic(to)
        result ? next(result) : next()
      })

      await router.push('/login')
      await router.isReady()

      expect(router.currentRoute.value.path).toBe('/login')
    })

    it('allows /square when no token', async () => {
      const router = createTestRouter()

      router.beforeEach((to, _from, next) => {
        const result = guardLogic(to)
        result ? next(result) : next()
      })

      await router.push('/square')
      await router.isReady()

      expect(router.currentRoute.value.path).toBe('/square')
    })

    it('allows /image/:uuid when no token', async () => {
      const router = createTestRouter()

      router.beforeEach((to, _from, next) => {
        const result = guardLogic(to)
        result ? next(result) : next()
      })

      await router.push('/image/400a1e49-6990-489e-b4a8-35eb0a02d056')
      await router.isReady()

      expect(router.currentRoute.value.path).toBe('/image/400a1e49-6990-489e-b4a8-35eb0a02d056')
    })
  })
})
