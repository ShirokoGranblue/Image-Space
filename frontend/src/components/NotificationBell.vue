<template>
  <button class="notification-bell" type="button" :aria-label="bellLabel" @click="toggle">
    <div class="bell-icon">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
        <path d="M13.73 21a2 2 0 0 1-3.46 0" />
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
const badgeValue = computed(() => unreadCount.value > 99 ? '99+' : unreadCount.value > 0 ? unreadCount.value : '')
const bellLabel = computed(() => unreadCount.value > 0 ? `通知，${unreadCount.value} 条未读` : '通知')

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

function toggle(event) {
  toggleNotificationDrawer(event.currentTarget)
  refreshUnread()
}
</script>

<style scoped>
.notification-bell {
  width: var(--control-height-md);
  height: var(--control-height-md);
  border: none;
  background: transparent;
  color: var(--color-text-secondary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color var(--duration-fast) var(--ease-standard), background-color var(--duration-fast) var(--ease-standard);
  padding: 0;
  border-radius: var(--radius-sm);
}

@media (max-width: 900px) {
  .notification-bell { width: var(--control-height-lg); height: var(--control-height-lg); }
}

.notification-bell:hover,
.notification-bell:focus-visible {
  background: var(--color-surface-2);
  color: var(--color-night);
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
  min-width: 16px;
  height: 16px;
  padding-inline: 3px;
  border-radius: 50%;
  background: var(--color-vermilion);
  color: var(--color-text-inverse);
  font-size: 10px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-body);
  line-height: 1;
}
</style>
