<template>
  <button class="notification-bell" type="button" title="通知" @click="toggle">
    <div class="bell-icon">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
        <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
      </svg>
      <span v-if="unreadCount > 0" class="bell-badge">{{ badgeValue }}</span>
    </div>
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
  border: none;
  background: transparent;
  color: var(--black);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color 0.2s ease;
  padding: 0;
  outline: none;
}

.notification-bell:hover {
  color: var(--anime-pink);
}

.bell-icon {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 0;
}

.bell-badge {
  position: absolute;
  top: -5px;
  right: -6px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--accent);
  color: var(--white);
  font-size: 15px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-body);
  line-height: 1;
  animation: badge-blink 1.4s ease-in-out infinite;
}

@keyframes badge-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}
</style>
