<template>
  <Teleport to="body">
    <transition name="notification-slide">
      <aside v-if="state.open" class="notification-drawer" aria-label="通知">
        <header class="notification-header">
          <div>
            <h2>通知</h2>
            <p>{{ unreadText }}</p>
          </div>
          <div class="notification-header-actions">
            <el-button text size="small" @click="handleReadAll" :disabled="unreadCount === 0">全部已读</el-button>
            <button class="drawer-close" type="button" @click="close" aria-label="关闭通知">×</button>
          </div>
        </header>

        <div class="notification-list" v-loading="loading">
          <button
            v-for="item in notifications"
            :key="item.id"
            class="notification-item"
            :class="{ unread: !item.read }"
            type="button"
            @click="openNotification(item)"
          >
            <img :src="item.imagePreviewUrl || fallbackImage" alt="" class="notification-thumb" />
            <span class="notification-body">
              <span class="notification-title">
                <strong>{{ item.actorName || '用户' }}</strong>
                {{ actionText(item) }}
                <strong>{{ item.imageName || '图片' }}</strong>
              </span>
              <span v-if="item.type === 'COMMENT' && item.contentPreview" class="notification-preview">
                {{ item.contentPreview }}
              </span>
              <span class="notification-time">{{ formatTime(item.createTime) }}</span>
            </span>
          </button>

          <div v-if="!loading && notifications.length === 0" class="notification-empty">
            暂无通知
          </div>
        </div>
      </aside>
    </transition>
  </Teleport>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, watch, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getNotifications, getUnreadNotificationCount, markAllNotificationsRead, markNotificationRead } from '../api/notification'
import { useNotificationDrawer, closeNotificationDrawer, setUnreadCount } from '../composables/useNotificationDrawer'
import { formatTime } from '../utils/format'

const router = useRouter()
const { state } = useNotificationDrawer()
const notifications = ref([])
const loading = ref(false)
const fallbackImage = 'data:image/gif;base64,R0lGODlhAQABAAD/ACwAAAAAAQABAAACADs='
const unreadCount = computed(() => state.unreadCount)
const unreadText = computed(() => unreadCount.value > 0 ? `${unreadCount.value} 条未读` : '全部已读')

watch(() => state.open, async (open) => {
  shiftApp(open)
  if (open) {
    await refresh()
  }
}, { immediate: true })

onBeforeUnmount(() => shiftApp(false))

async function refresh() {
  loading.value = true
  try {
    const [listRes, countRes] = await Promise.all([
      getNotifications({ page: 1, limit: 20, unreadOnly: false }),
      getUnreadNotificationCount(),
    ])
    notifications.value = listRes.data?.records || []
    setUnreadCount(countRes.data || 0)
  } catch {
  } finally {
    loading.value = false
  }
}

async function openNotification(item) {
  try {
    if (!item.read) {
      await markNotificationRead(item.id)
      item.read = true
      setUnreadCount(Math.max(0, unreadCount.value - 1))
    }
    close()
    await nextTick()
    router.push(item.targetUrl || `/image/${item.imageId}`)
  } catch {}
}

async function handleReadAll() {
  try {
    await markAllNotificationsRead()
    notifications.value = notifications.value.map(item => ({ ...item, read: true }))
    setUnreadCount(0)
  } catch {}
}

function close() {
  closeNotificationDrawer()
}

function shiftApp(open) {
  const app = document.getElementById('app')
  if (!app) return
  app.classList.toggle('notification-drawer-open', open)
}

function actionText(item) {
  return item.type === 'LIKE' ? ' 点赞了你的图片 ' : ' 评论了你的图片 '
}
</script>

<style scoped>
.notification-drawer {
  position: fixed;
  top: 0;
  right: 0;
  z-index: 2000;
  width: var(--notification-drawer-width, 420px);
  height: 100dvh;
  background: rgba(255, 255, 255, 0.96);
  border-left: 1px solid rgba(203, 213, 225, 0.78);
  box-shadow: -28px 0 56px rgba(15, 23, 42, 0.16);
  backdrop-filter: saturate(180%) blur(18px);
  -webkit-backdrop-filter: saturate(180%) blur(18px);
  display: flex;
  flex-direction: column;
}

.notification-header {
  padding: 20px 22px 16px;
  border-bottom: 1px solid var(--border-subtle);
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.notification-header h2 {
  margin: 0;
  font-family: var(--font-display);
  color: var(--text-primary);
  font-size: 22px;
  letter-spacing: 0;
}

.notification-header p {
  margin: 4px 0 0;
  color: var(--text-muted);
  font-size: 13px;
}

.notification-header-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.drawer-close {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid var(--border-subtle);
  background: #fff;
  color: var(--text-muted);
  cursor: pointer;
  font-size: 20px;
  line-height: 1;
}

.drawer-close:hover {
  color: var(--accent);
  border-color: rgba(37, 99, 235, 0.24);
}

.notification-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.notification-item {
  width: 100%;
  height: 96px;
  border: 1px solid transparent;
  border-radius: 12px;
  background: transparent;
  display: grid;
  grid-template-columns: 72px 1fr;
  gap: 12px;
  padding: 10px;
  cursor: pointer;
  text-align: left;
  transition: background 0.18s ease, border-color 0.18s ease, transform 0.18s ease;
}

.notification-item:hover {
  background: rgba(239, 244, 255, 0.88);
  border-color: rgba(37, 99, 235, 0.14);
  transform: translateX(-2px);
}

.notification-item.unread {
  background: rgba(37, 99, 235, 0.06);
}

.notification-thumb {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border-radius: 10px;
  border: 1px solid var(--border-subtle);
  background: var(--bg-elevated);
}

.notification-body {
  min-width: 0;
  display: grid;
  grid-template-rows: auto 1fr auto;
  gap: 4px;
}

.notification-title {
  color: var(--text-primary);
  font-size: 14px;
  line-height: 1.35;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.notification-preview {
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.3;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.notification-time {
  justify-self: end;
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 600;
}

.notification-empty {
  height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  font-size: 14px;
}

.notification-slide-enter-active,
.notification-slide-leave-active {
  transition: transform 0.18s ease, opacity 0.18s ease;
}

.notification-slide-enter-from,
.notification-slide-leave-to {
  transform: translateX(100%);
  opacity: 0;
}
</style>
