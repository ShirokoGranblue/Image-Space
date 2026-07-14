import { describe, it, expect, beforeEach } from 'vitest'
import { createRouter, createWebHistory, type RouteLocationNormalized } from 'vue-router'
import { isAllowedAdminDomain } from '../utils/adminDomain'

function createTestRouter() {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/login', name: 'Login', component: { template: '<div>Login</div>' } },
      { path: '/register', name: 'Register', component: { template: '<div>Register</div>' } },
      { path: '/403', name: 'Forbidden', component: { template: '<div>Forbidden</div>' } },
      { path: '/', redirect: '/home' },
      { path: '/home', name: 'Home', component: { template: '<div>Home</div>' }, meta: { requiresAuth: true } },
      { path: '/square', name: 'ImageSquare', component: { template: '<div>Square</div>' } },
      { path: '/image/:uuid', name: 'ImageDetail', component: { template: '<div>Detail</div>' } },
      { path: '/profile/:uuid', name: 'Profile', component: { template: '<div>Profile</div>' } },
      {
        path: '/admin/audit-log',
        name: 'AuditLogMonitor',
        component: { template: '<div>Audit</div>' },
        meta: { requiresAuth: true, requiresAdmin: true, requiresAdminDomain: true },
      },
      { path: '/:pathMatch(.*)*', name: 'NotFound', component: { template: '<div>Not Found</div>' } },
    ],
  })
}

function guardLogic(
  to: RouteLocationNormalized,
  options: { hostname?: string; allowLocal?: boolean; userRole?: string | null } = {},
): string | null {
  const token = sessionStorage.getItem('satoken')
  if (to.meta.requiresAdminDomain && !isAllowedAdminDomain(options.hostname || 'admin.image-space.app', options.allowLocal ?? false)) {
    return '/403'
  }
  if (to.meta.requiresAuth && !token) return '/login'
  if ((to.path === '/login' || to.path === '/register') && token) return '/home'
  if (to.meta.requiresAdmin && options.userRole !== 'admin') return '/403'
  return null
}

describe('Router guard logic', () => {
  beforeEach(() => {
    sessionStorage.clear()
  })

  it('redirects auth-required routes to /login when no token', async () => {
    const router = createTestRouter()
    router.beforeEach((to, _from, next) => {
      const result = guardLogic(to)
      result ? next(result) : next()
    })

    await router.push('/home')
    await router.isReady()

    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('allows auth-required routes when token exists', async () => {
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

  it('redirects public auth pages to /home when already logged in', async () => {
    sessionStorage.setItem('satoken', 'valid-token')
    const router = createTestRouter()
    router.beforeEach((to, _from, next) => {
      const result = guardLogic(to)
      result ? next(result) : next()
    })

    await router.push('/login')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/home')

    await router.push('/register')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/home')
  })

  it('allows public routes without token', async () => {
    const router = createTestRouter()
    router.beforeEach((to, _from, next) => {
      const result = guardLogic(to)
      result ? next(result) : next()
    })

    await router.push('/square')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/square')

    await router.push('/image/400a1e49-6990-489e-b4a8-35eb0a02d056')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/image/400a1e49-6990-489e-b4a8-35eb0a02d056')
  })

  it('blocks admin page on a non-admin domain before login checks', async () => {
    const router = createTestRouter()
    router.beforeEach((to, _from, next) => {
      const result = guardLogic(to, { hostname: 'image-space.app', allowLocal: false, userRole: 'admin' })
      result ? next(result) : next()
    })

    await router.push('/admin/audit-log')
    await router.isReady()

    expect(router.currentRoute.value.path).toBe('/403')
  })

  it('blocks admin page for logged-in non-admin users', async () => {
    sessionStorage.setItem('satoken', 'valid-token')
    const router = createTestRouter()
    router.beforeEach((to, _from, next) => {
      const result = guardLogic(to, { hostname: 'admin.image-space.app', allowLocal: false, userRole: 'user' })
      result ? next(result) : next()
    })

    await router.push('/admin/audit-log')
    await router.isReady()

    expect(router.currentRoute.value.path).toBe('/403')
  })

  it('allows admin page for admin users on the admin domain', async () => {
    sessionStorage.setItem('satoken', 'valid-token')
    const router = createTestRouter()
    router.beforeEach((to, _from, next) => {
      const result = guardLogic(to, { hostname: 'admin.image-space.app', allowLocal: false, userRole: 'admin' })
      result ? next(result) : next()
    })

    await router.push('/admin/audit-log')
    await router.isReady()

    expect(router.currentRoute.value.path).toBe('/admin/audit-log')
  })

  it('renders the explicit not-found route for /categories', async () => {
    const router = createTestRouter()

    await router.push('/categories')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('NotFound')
  })
})
