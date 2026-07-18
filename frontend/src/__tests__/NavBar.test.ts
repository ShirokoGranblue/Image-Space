import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'

// Mock vue-router
const mockPush = vi.fn()
const adminDomainState = vi.hoisted(() => ({ allowed: false }))
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockPush }),
  RouterLink: {
    name: 'RouterLink',
    props: ['to'],
    template: '<a :href="to"><slot /></a>',
  },
}))

vi.mock('../utils/adminDomain', () => ({
  isAllowedAdminDomain: () => adminDomainState.allowed,
}))

// Mock pinia user store
const mockUserInfo = { id: 1, username: 'testuser', displayName: 'Test', avatar: '/avatar.jpg', role: 'user' }
vi.mock('../store/user', () => ({
  useUserStore: vi.fn(() => ({
    token: 'fake-token',
    userInfo: mockUserInfo,
    fetchUserInfo: vi.fn(),
    clearToken: vi.fn(),
  })),
}))

// Mock logout API
vi.mock('../api/user', () => ({
  logout: vi.fn(() => Promise.resolve()),
}))

vi.mock('../api/notification', () => ({
  getUnreadNotificationCount: vi.fn(() => Promise.resolve({ data: 0 })),
}))

// Stub element-plus components to avoid teleport/overlay issues
const ElHeader = { name: 'ElHeader', template: '<header><slot /></header>' }
const ElAvatar = {
  name: 'ElAvatar',
  props: ['src', 'size'],
  template: '<div class="el-avatar-mock"><slot /></div>',
}

import NavBar from '../components/NavBar.vue'

function mountNavBar(routePath = '/home') {
  return mount(NavBar, {
    global: {
      stubs: {
        'el-header': ElHeader,
        'el-avatar': ElAvatar,
        'el-icon': { name: 'ElIcon', template: '<i><slot /></i>' },
        'router-link': {
          name: 'RouterLink',
          props: ['to'],
          template: '<a :href="to" :class="$attrs.class"><slot /></a>',
        },
        'router-view': true,
        'transition': { name: 'Transition', template: '<div v-if="$attrs"><slot /></div>' },
        NotificationBell: { template: '<div class="notification-bell" />' },
      },
      mocks: {
        $route: { path: routePath },
      },
    },
  })
}

describe('NavBar', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    adminDomainState.allowed = false
    mockUserInfo.role = 'user'
    // Reset viewport to desktop
    Object.defineProperty(window, 'innerWidth', { value: 1024, writable: true })
  })

  describe('Logo', () => {
    it('renders the product logo linking to home', () => {
      const wrapper = mountNavBar()
      const logo = wrapper.find('.logo')
      expect(logo.exists()).toBe(true)
      expect(logo.text()).toContain('AstralSpace')
      expect(logo.attributes('href')).toBe('/home')
    })

    it('uses a text-only wordmark without the former icon or subtitle', () => {
      const wrapper = mountNavBar()
      expect(wrapper.find('.logo-wordmark').text()).toBe('AstralSpace')
      expect(wrapper.find('.logo-mark').exists()).toBe(false)
      expect(wrapper.find('.logo small').exists()).toBe(false)
    })
  })

  describe('Desktop navigation', () => {
    it('renders nav links when logged in', () => {
      const wrapper = mountNavBar()
      const links = wrapper.findAll('.nav-link')
      expect(links.map(link => link.text())).toEqual(['Explore', 'Images', 'Profile'])
    })

    it('highlights the active route', () => {
      const wrapper = mountNavBar('/home')
      const homeLink = wrapper.findAll('.nav-link').find(l => l.attributes('href') === '/home')
      expect(homeLink?.classes()).toContain('active')
    })

    it('highlights square route when active', () => {
      const wrapper = mountNavBar('/square')
      const squareLink = wrapper.findAll('.nav-link').find(l => l.attributes('href') === '/square')
      expect(squareLink?.classes()).toContain('active')
    })

    it('shows the audit entry only for admins on the admin domain', () => {
      mockUserInfo.role = 'admin'
      adminDomainState.allowed = true

      const wrapper = mountNavBar('/admin/audit-log')
      const auditLink = wrapper.findAll('.nav-link').find(link => link.attributes('href') === '/admin/audit-log')

      expect(auditLink?.text()).toBe('Audit')
      expect(auditLink?.classes()).toContain('active')
    })

    it('does not show the audit entry for an admin on the ordinary domain', () => {
      mockUserInfo.role = 'admin'
      adminDomainState.allowed = false

      const wrapper = mountNavBar()

      expect(wrapper.findAll('.nav-link').some(link => link.attributes('href') === '/admin/audit-log')).toBe(false)
    })
  })

  describe('Mobile menu toggle', () => {
    it('shows mobile toggle button', () => {
      // Mock mobile viewport
      Object.defineProperty(window, 'innerWidth', { value: 375, writable: true })
      const wrapper = mountNavBar()
      const toggle = wrapper.find('.mobile-toggle')
      expect(toggle.exists()).toBe(true)
      expect(toggle.attributes('aria-label')).toBe('菜单')
    })

    it('toggles aria-expanded when clicked', async () => {
      Object.defineProperty(window, 'innerWidth', { value: 375, writable: true })
      const wrapper = mountNavBar()
      const toggle = wrapper.find('.mobile-toggle')

      expect(toggle.attributes('aria-expanded')).toBe('false')

      await toggle.trigger('click')
      await nextTick()
      expect(toggle.attributes('aria-expanded')).toBe('true')

      await toggle.trigger('click')
      await nextTick()
      expect(toggle.attributes('aria-expanded')).toBe('false')
    })

    it('shows mobile drawer when toggle is clicked', async () => {
      Object.defineProperty(window, 'innerWidth', { value: 375, writable: true })
      const wrapper = mountNavBar()

      // Drawer should not exist initially
      expect(wrapper.find('.mobile-drawer').exists()).toBe(false)

      // Click toggle
      await wrapper.find('.mobile-toggle').trigger('click')
      await nextTick()

      // Drawer should now exist
      expect(wrapper.find('.mobile-drawer').exists()).toBe(true)
    })
  })

  describe('Mobile drawer', () => {
    it('renders navigation links in mobile drawer', async () => {
      Object.defineProperty(window, 'innerWidth', { value: 375, writable: true })
      const wrapper = mountNavBar()

      await wrapper.find('.mobile-toggle').trigger('click')
      await nextTick()

      const mobileLinks = wrapper.findAll('.mobile-nav-item')
      expect(mobileLinks.length).toBeGreaterThanOrEqual(2)
      expect(mobileLinks.map(link => link.text())).toEqual(['Explore', 'Images', 'Profile'])
    })
  })

  describe('User section', () => {
    it('displays avatar button when logged in', () => {
      const wrapper = mountNavBar()
      const avatarBtn = wrapper.find('.avatar-button')
      expect(avatarBtn.exists()).toBe(true)
    })

    it('displays logout button when logged in', () => {
      const wrapper = mountNavBar()
      const logoutBtn = wrapper.find('.logout-btn')
      expect(logoutBtn.exists()).toBe(true)
      expect(logoutBtn.text()).toBe('Exit')
    })
  })
})
