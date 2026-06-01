import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'

const mockPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockPush }),
}))

vi.mock('../api/notification', () => ({
  getNotifications: vi.fn(() => Promise.resolve({
    data: {
      records: [
        {
          id: 1,
          type: 'COMMENT',
          actorName: 'Alice',
          imageName: 'summer.jpg',
          imagePreviewUrl: '/api/image/download/400a1e49-6990-489e-b4a8-35eb0a02d056',
          imageId: 1,
          imageUuid: '400a1e49-6990-489e-b4a8-35eb0a02d056',
          commentId: 9,
          contentPreview: 'nice pic',
          read: false,
          createTime: '2026-05-26T10:00:00',
          targetUrl: '/image/400a1e49-6990-489e-b4a8-35eb0a02d056?notificationId=1&commentId=9&highlight=comment',
        },
      ],
      total: 1,
    },
  })),
  getUnreadNotificationCount: vi.fn(() => Promise.resolve({ data: 1 })),
  markNotificationRead: vi.fn(() => Promise.resolve({ data: null })),
  markAllNotificationsRead: vi.fn(() => Promise.resolve({ data: null })),
}))

import NotificationDrawer from '../components/NotificationDrawer.vue'
import { closeNotificationDrawer, openNotificationDrawer } from '../composables/useNotificationDrawer'
import { markNotificationRead } from '../api/notification'

describe('NotificationDrawer', () => {
  beforeEach(() => {
    document.body.innerHTML = '<div id="app"></div>'
    mockPush.mockClear()
  })

  afterEach(() => {
    closeNotificationDrawer()
    document.body.innerHTML = ''
  })

  it('pushes app content left while open and restores it when closed', async () => {
    mount(NotificationDrawer, {
      attachTo: document.body,
      global: {
        stubs: {
          'el-icon': { template: '<i />' },
          'el-button': { template: '<button @click="$emit(\'click\')"><slot /></button>' },
          'el-badge': { template: '<span><slot /></span>' },
          'el-checkbox': { template: '<input type="checkbox" />', props: ['modelValue'] },
        },
      },
    })

    openNotificationDrawer()
    await flushPromises()
    await nextTick()

    expect(document.querySelector('.notification-drawer')).toBeTruthy()
    expect(document.getElementById('app')?.classList.contains('notification-drawer-open')).toBe(true)

    closeNotificationDrawer()
    await nextTick()

    expect(document.getElementById('app')?.classList.contains('notification-drawer-open')).toBe(false)
  })

  it('marks notification read and routes to target when clicked', async () => {
    mount(NotificationDrawer, {
      attachTo: document.body,
      global: {
        stubs: {
          'el-icon': { template: '<i />' },
          'el-button': { template: '<button @click="$emit(\'click\')"><slot /></button>' },
          'el-badge': { template: '<span><slot /></span>' },
          'el-checkbox': { template: '<input type="checkbox" />', props: ['modelValue'] },
        },
      },
    })

    openNotificationDrawer()
    await flushPromises()

    document.querySelector<HTMLButtonElement>('.notification-item-content')?.click()
    await flushPromises()

    expect(markNotificationRead).toHaveBeenCalledWith(1)
    expect(mockPush).toHaveBeenCalledWith('/image/400a1e49-6990-489e-b4a8-35eb0a02d056?notificationId=1&commentId=9&highlight=comment')
  })
})
