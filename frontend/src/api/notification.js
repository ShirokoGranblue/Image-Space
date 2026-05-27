import api from './index'

export function getNotifications(params = {}) {
  return api.get('/notification/list', { params })
}

export function getUnreadNotificationCount() {
  return api.get('/notification/unread-count')
}

export function markNotificationRead(id) {
  return api.put(`/notification/${id}/read`)
}

export function markAllNotificationsRead() {
  return api.put('/notification/read-all')
}
