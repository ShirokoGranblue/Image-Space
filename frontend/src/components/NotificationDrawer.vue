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
          <div
            v-for="item in notifications"
            :key="item.id"
            class="notification-item"
            :class="{ unread: !item.read }"
          >
            <el-checkbox
              class="notification-checkbox"
              :model-value="selectedIds.has(item.id)"
              @change="toggleSelect(item.id)"
              @click.stop
            />
            <button
              class="notification-item-content"
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
                <span v-if="(item.type === 'COMMENT' || item.type === 'COMMENT_LIKE') && item.contentPreview" class="notification-preview">
                  {{ item.contentPreview }}
                </span>
                <span class="notification-time">{{ formatTime(item.createTime) }}</span>
              </span>
            </button>
          </div>

          <div v-if="!loading && notifications.length === 0" class="notification-empty">
            <span class="empty-icon">◇</span>
            <span>暂无通知</span>
          </div>
        </div>

        <footer class="notification-footer" v-if="notifications.length > 0">
          <el-button
            size="small"
            :disabled="selectedIds.size === 0"
            @click="clearSelection"
          >取消</el-button>
          <el-button
            size="small"
            type="danger"
            :disabled="selectedIds.size === 0"
            @click="handleDeleteSelected"
          >删除选中({{ selectedIds.size }})</el-button>
        </footer>
      </aside>
    </transition>
  </Teleport>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, watch, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getNotifications, getUnreadNotificationCount, markAllNotificationsRead, markNotificationRead, deleteNotification, deleteNotifications } from '../api/notification'
import { useNotificationDrawer, closeNotificationDrawer, setUnreadCount } from '../composables/useNotificationDrawer'
import { formatTime } from '../utils/format'

const router = useRouter()
const { state } = useNotificationDrawer()
const notifications = ref([])
const loading = ref(false)
const selectedIds = ref(new Set())
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
    const target = item.targetUrl || (item.imageUuid ? `/image/${item.imageUuid}` : '')
    if (target) {
      router.push(target)
    }
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
  if (item.type === 'LIKE') return ' 点赞了你的图片 '
  if (item.type === 'COMMENT_LIKE') return ' 点赞了你的评论 '
  return ' 评论了你的图片 '
}

function toggleSelect(id) {
  const next = new Set(selectedIds.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
    next.add(id)
  }
  selectedIds.value = next
}

function clearSelection() {
  selectedIds.value = new Set()
}

async function handleDeleteSelected() {
  if (selectedIds.value.size === 0) return
  try {
    const ids = [...selectedIds.value]
    if (ids.length === 1) {
      await deleteNotification(ids[0])
    } else {
      await deleteNotifications(ids)
    }
    clearSelection()
    await refresh()
  } catch {}
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
  background: var(--white);
  border-left: 1px solid var(--gray2);
  display: flex;
  flex-direction: column;
}

.notification-header {
  padding: 24px 24px 16px;
  border-bottom: 1px solid var(--gray2);
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.notification-header h2 {
  margin: 0;
  font-family: var(--font-display);
  color: var(--black);
  font-size: 26px;
  letter-spacing: 0.03em;
  line-height: 1;
  font-weight: 400;
}

.notification-header p {
  margin: 6px 0 0;
  color: var(--gray3);
  font-size: 13px;
  font-weight: 400;
}

.notification-header-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.drawer-close {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: var(--gray3);
  cursor: pointer;
  font-size: 22px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s ease;
  padding: 0;
}

.drawer-close:hover {
  color: var(--black);
}

.notification-list {
  flex: 1;
  overflow-y: auto;
}

.notification-item {
  display: flex;
  align-items: stretch;
  gap: 12px;
  padding: 16px 24px;
  border-bottom: 1px solid var(--gray2);
  border-left: 2px solid transparent;
  background: transparent;
  transition: background 0.2s ease, border-left-color 0.2s ease, transform 0.2s ease;
}

.notification-item:hover {
  background: var(--white);
  border-left-color: var(--accent);
  transform: translateX(-4px);
}

.notification-item.unread {
  background: var(--gray1);
}

.notification-item.unread:hover {
  background: var(--gray1);
  border-left-color: var(--accent);
  transform: translateX(-4px);
}

.notification-checkbox {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  padding: 22px 0;
}

.notification-item-content {
  flex: 1;
  min-width: 0;
  border: none;
  background: transparent;
  display: grid;
  grid-template-columns: 64px 1fr;
  gap: 14px;
  padding: 0;
  cursor: pointer;
  text-align: left;
  font: inherit;
  color: inherit;
}

.notification-thumb {
  width: 64px;
  height: 64px;
  object-fit: cover;
  background: var(--gray1);
}

.notification-body {
  min-width: 0;
  display: grid;
  grid-template-rows: auto 1fr auto;
  gap: 4px;
  padding: 2px 0;
}

.notification-title {
  color: var(--black);
  font-size: 14px;
  line-height: 1.4;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.notification-title strong {
  font-weight: 600;
}

.notification-preview {
  color: var(--gray3);
  font-size: 13px;
  line-height: 1.3;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.notification-time {
  color: var(--gray3);
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.notification-empty {
  padding: 80px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  color: var(--gray3);
  font-size: 14px;
  font-family: var(--font-body);
}

.empty-icon {
  font-size: 32px;
  line-height: 1;
  opacity: 0.4;
}

.notification-footer {
  padding: 12px 24px;
  border-top: 1px solid var(--gray2);
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.notification-slide-enter-active,
.notification-slide-leave-active {
  transition: transform 0.25s var(--ease-out), opacity 0.25s var(--ease-out);
}

.notification-slide-enter-from,
.notification-slide-leave-to {
  transform: translateX(100%);
  opacity: 0;
}
</style>
