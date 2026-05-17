import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'

// Mock vue-router
const mockPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockPush }),
  RouterLink: {
    name: 'RouterLink',
    props: ['to'],
    template: '<a :href="to"><slot /></a>',
  },
}))

// Mock pinia user store
const mockUserInfo = { id: 1, username: 'testuser', displayName: 'Test', avatar: '/avatar.jpg' }
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
    // Reset viewport to desktop
    Object.defineProperty(window, 'innerWidth', { value: 1024, writable: true })
  })

  describe('Logo', () => {
    it('renders the ImageSpace logo linking to home', () => {
      const wrapper = mountNavBar()
      const logo = wrapper.find('.logo')
      expect(logo.exists()).toBe(true)
      expect(logo.text()).toBe('ImageSpace')
      expect(logo.attributes('href')).toBe('/home')
    })
  })

  describe('Desktop navigation', () => {
    it('renders both nav links', () => {
      const wrapper = mountNavBar()
      const links = wrapper.findAll('.nav-link')
      expect(links).toHaveLength(2)
    })

    it('highlights the active route', () => {
      const wrapper = mountNavBar('/home')
      const homeLink = wrapper.find('.nav-link.active')
      expect(homeLink.exists()).toBe(true)
    })

    it('does not highlight inactive route', () => {
      const wrapper = mountNavBar('/square')
      const homeLink = wrapper.find('.nav-link.active')
      expect(homeLink.exists()).toBe(true)
      expect(homeLink.find('.nav-label').text()).toBe('图片广场')
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
      expect(mobileLinks).toHaveLength(2)
      expect(mobileLinks[0].text()).toContain('我的图片')
      expect(mobileLinks[1].text()).toContain('图片广场')
    })
  })

  describe('User section', () => {
    it('displays user name and avatar', () => {
      const wrapper = mountNavBar()
      const username = wrapper.find('.username')
      expect(username.exists()).toBe(true)
      expect(username.text()).toBe('Test')
    })
  })
})
