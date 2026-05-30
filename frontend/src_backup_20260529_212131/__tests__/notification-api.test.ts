import { describe, expect, it, vi } from 'vitest'

vi.mock('../api/index', () => ({
  default: {
    get: vi.fn(),
    put: vi.fn(),
  },
}))

import api from '../api/index'
import {
  getNotifications,
  getUnreadNotificationCount,
  markAllNotificationsRead,
  markNotificationRead,
} from '../api/notification'

describe('notification api', () => {
  it('loads notifications with page params', () => {
    getNotifications({ page: 2, limit: 20, unreadOnly: true })

    expect(api.get).toHaveBeenCalledWith('/notification/list', {
      params: { page: 2, limit: 20, unreadOnly: true },
    })
  })

  it('marks one or all notifications as read', () => {
    markNotificationRead(7)
    markAllNotificationsRead()

    expect(api.put).toHaveBeenCalledWith('/notification/7/read')
    expect(api.put).toHaveBeenCalledWith('/notification/read-all')
  })

  it('loads unread count', () => {
    getUnreadNotificationCount()

    expect(api.get).toHaveBeenCalledWith('/notification/unread-count')
  })
})
