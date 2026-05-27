import { reactive } from 'vue'

const state = reactive({
  open: false,
  unreadCount: 0,
})

export function useNotificationDrawer() {
  return {
    state,
    open: openNotificationDrawer,
    close: closeNotificationDrawer,
    toggle: toggleNotificationDrawer,
    setUnreadCount,
  }
}

export function openNotificationDrawer() {
  state.open = true
}

export function closeNotificationDrawer() {
  state.open = false
}

export function toggleNotificationDrawer() {
  state.open = !state.open
}

export function setUnreadCount(count) {
  state.unreadCount = Number(count) || 0
}
