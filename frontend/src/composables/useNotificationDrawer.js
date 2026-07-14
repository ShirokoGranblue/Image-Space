import { reactive } from 'vue'

const state = reactive({
  open: false,
  unreadCount: 0,
})
let triggerElement = null

export function useNotificationDrawer() {
  return {
    state,
    open: openNotificationDrawer,
    close: closeNotificationDrawer,
    toggle: toggleNotificationDrawer,
    setUnreadCount,
  }
}

export function openNotificationDrawer(element) {
  triggerElement = element && typeof element.focus === 'function' ? element : null
  state.open = true
}

export function closeNotificationDrawer() {
  state.open = false
  const trigger = triggerElement
  triggerElement = null
  if (trigger?.isConnected) queueMicrotask(() => trigger.focus())
}

export function toggleNotificationDrawer(element) {
  if (state.open) closeNotificationDrawer()
  else openNotificationDrawer(element)
}

export function setUnreadCount(count) {
  state.unreadCount = Number(count) || 0
}
