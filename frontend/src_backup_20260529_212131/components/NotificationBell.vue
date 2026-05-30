<template>
  <button class="notification-bell" type="button" title="通知" @click="toggle">
    <el-badge :value="badgeValue" :hidden="unreadCount === 0" :max="99">
      <el-icon><Bell /></el-icon>
    </el-badge>
  </button>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, watch } from 'vue'
import { getUnreadNotificationCount } from '../api/notification'
import { useNotificationDrawer, setUnreadCount, toggleNotificationDrawer } from '../composables/useNotificationDrawer'

const props = defineProps({
  active: { type: Boolean, default: false },
})

const { state } = useNotificationDrawer()
let timer = null

const unreadCount = computed(() => state.unreadCount)
const badgeValue = computed(() => unreadCount.value > 0 ? unreadCount.value : '')

onMounted(() => {
  if (props.active) start()
  document.addEventListener('visibilitychange', handleVisibility)
})

onBeforeUnmount(() => {
  stop()
  document.removeEventListener('visibilitychange', handleVisibility)
})

watch(() => props.active, (active) => {
  if (active) start()
  else {
    stop()
    setUnreadCount(0)
  }
})

async function refreshUnread() {
  if (!props.active || document.hidden) return
  try {
    const res = await getUnreadNotificationCount()
    setUnreadCount(res.data || 0)
  } catch {}
}

function start() {
  if (timer) return
  refreshUnread()
  timer = window.setInterval(refreshUnread, 15000)
}

function stop() {
  if (timer) {
    window.clearInterval(timer)
    timer = null
  }
}

function handleVisibility() {
  if (document.hidden) stop()
  else if (props.active) start()
}

function toggle() {
  toggleNotificationDrawer()
  refreshUnread()
}
</script>

<style scoped>
.notification-bell {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 1px solid rgba(203, 213, 225, 0.72);
  background: rgba(255, 255, 255, 0.82);
  color: var(--text-muted);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color 0.18s ease, border-color 0.18s ease, background 0.18s ease, transform 0.18s ease;
}

.notification-bell:hover {
  color: var(--accent);
  border-color: rgba(37, 99, 235, 0.24);
  background: #fff;
  transform: translateY(-1px);
}

.notification-bell :deep(.el-icon) {
  font-size: 18px;
}
</style>
